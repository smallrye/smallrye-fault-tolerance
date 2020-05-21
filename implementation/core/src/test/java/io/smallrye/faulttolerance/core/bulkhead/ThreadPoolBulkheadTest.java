package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class ThreadPoolBulkheadTest {
    private final ExecutorService executor = executor(4, queue(4));

    @Test
    public void shouldLetSingleThrough() throws Exception {
        TestInvocation<Future<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedFuture("shouldLetSingleThrough"));
        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldLetSingleThrough", executor, 2, 2);
        Future<String> result = bulkhead.apply(new InvocationContext<>(null));
        assertThat(result.get()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxThrough() throws Exception { // max threads + max queue
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<Future<String>> invocation = TestInvocation.delayed(delayBarrier,
                () -> completedFuture("shouldLetMaxThrough"));
        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldLetSingleThrough", executor, 2, 3);

        List<TestThread<Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await().get()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Barrier startBarrier = Barrier.noninterruptible();

        TestInvocation<Future<String>> invocation = TestInvocation.delayed(startBarrier, delayBarrier,
                () -> completedFuture("shouldRejectMaxPlus1"));
        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldRejectMaxPlus1", executor, 2, 3);

        List<TestThread<Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        waitUntilQueueSize(bulkhead, 3, 500);

        assertThatThrownBy(() -> bulkhead.apply(new InvocationContext<>(null)))
                .isInstanceOf(BulkheadException.class);

        delayBarrier.open();
        for (int i = 0; i < 5; i++) {
            assertThat(threads.get(i).await().get()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);
        Semaphore finishedThreadsCount = new Semaphore(0);

        TestInvocation<Future<String>> invocation = TestInvocation.delayed(delayBarrier, () -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            return completedFuture("shouldLetMaxPlus1After1Left");
        });

        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", executor,
                2, 3);

        List<TestThread<Future<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();

        TestThread<Future<String>> finishedThread = getSingleFinishedThread(threads, 1000L);
        assertThat(finishedThread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        threads.remove(finishedThread);

        threads.add(runOnTestThread(bulkhead));

        letOneInSemaphore.release(5);
        for (TestThread<Future<String>> thread : threads) {
            finishedThreadsCount.acquire();
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Semaphore letOneInSemaphore = new Semaphore(0);
        Semaphore finishedThreadsCount = new Semaphore(0);

        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            throw error;
        });

        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", executor,
                2, 3);

        List<TestThread<Future<String>>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(runOnTestThread(bulkhead));
        }

        letOneInSemaphore.release();
        finishedThreadsCount.acquire();

        TestThread<Future<String>> finishedThread = getSingleFinishedThread(threads, 1000L);
        assertThatThrownBy(finishedThread::await).isEqualTo(error);
        threads.remove(finishedThread);

        threads.add(runOnTestThread(bulkhead));

        letOneInSemaphore.release(5);
        for (TestThread<Future<String>> thread : threads) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(thread::await).isEqualTo(error);
        }
    }

    /*
     * put five elements into the bulkhead,
     * check another one cannot be inserted
     * cancel one,
     * insert another one
     * run the tasks and check results
     */
    @Test
    public void shouldLetMaxPlus1After1Canceled() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();
        CountDownLatch invocationsStarted = new CountDownLatch(2);

        TestInvocation<Future<String>> invocation = TestInvocation.immediatelyReturning(() -> {
            invocationsStarted.countDown();
            delayBarrier.await();
            return completedFuture("shouldLetMaxPlus1After1Canceled");
        });

        ThreadPoolBulkhead<String> bulkhead = new ThreadPoolBulkhead<>(invocation, "shouldLetMaxPlus1After1Canceled", executor,
                2, 3);

        List<TestThread<Future<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            threads.add(runOnTestThread(bulkhead));
        }
        // to make sure the fifth added is enqueued, wait until two are started
        invocationsStarted.await();

        threads.add(runOnTestThread(bulkhead));

        waitUntilQueueSize(bulkhead, 3, 1000);

        TestThread<Future<String>> failedThread = runOnTestThread(bulkhead);
        assertThatThrownBy(failedThread::await).isInstanceOf(BulkheadException.class);

        threads.remove(4).interrupt(); // cancel and remove from the list
        waitUntilQueueSize(bulkhead, 2, 1000);

        threads.add(runOnTestThread(bulkhead));
        waitUntilQueueSize(bulkhead, 3, 1000);

        delayBarrier.open();

        for (TestThread<Future<String>> thread : threads) {
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Canceled");
        }
    }

    private void waitUntilQueueSize(ThreadPoolBulkhead<String> bulkhead, int size, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(50);
            if (bulkhead.getQueueSize() == size) {
                return;
            }
        }
        fail("queue not filled in in " + timeoutMs + " [ms], queue size: " + bulkhead.getQueueSize());

    }

    private <V> TestThread<Future<V>> getSingleFinishedThread(List<TestThread<Future<V>>> threads, long timeout)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(50);
            for (TestThread<Future<V>> thread : threads) {
                if (thread.isDone()) {
                    return thread;
                }
            }
        }
        fail("No thread finished in " + timeout + " ms");
        throw new AssertionError(); // dead code
    }

    private ThreadPoolExecutor executor(int size, BlockingQueue<Runnable> queue) {
        return new ThreadPoolExecutor(size, size, 0, TimeUnit.MILLISECONDS, queue);
    }

    private LinkedBlockingQueue<Runnable> queue(int capacity) {
        return new LinkedBlockingQueue<>(capacity);
    }
}
