package com.github.ladicek.oaken_ocean.core.bulkhead;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.Cancellator;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.FutureTestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureBulkheadTest {
    @Test
    public void shouldLetSingleThrough() throws Exception {
        FutureTestInvocation<String> invocation = FutureTestInvocation
                .immediatelyReturning(() -> completedFuture("shouldLetSingleThrough"));
        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldLetSingleThrough", 2, 2, null);
        Future<String> result = bulkhead.apply(new FutureInvocationContext<>(null, null));
        assertThat(result.get()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxThrough() throws Exception { // max threads + max queue
        Barrier delayBarrier = Barrier.noninterruptible();
        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(delayBarrier,
                () -> completedFuture("shouldLetMaxThrough"));
        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldLetSingleThrough", 2, 3, null);

        List<FutureTestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));
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

        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(startBarrier, delayBarrier,
                () -> completedFuture("shouldRejectMaxPlus1"));
        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldRejectMaxPlus1", 2, 3, null);

        List<FutureTestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));
        }

        assertThatThrownBy(() -> bulkhead.apply(new FutureInvocationContext<>(null, null)))
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

        FutureTestInvocation<String> invocation = FutureTestInvocation.delayed(delayBarrier, () -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            return CompletableFuture.completedFuture("shouldLetMaxPlus1After1Left");
        });

        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 2, 3, null);

        List<FutureTestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));
        }

        delayBarrier.open();
        finishedThreadsCount.acquire();

        FutureTestThread<String> finishedThread = getSingleFinishedThread(threads, 100L);
        assertThat(finishedThread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        threads.remove(finishedThread);

        threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));

        letOneInSemaphore.release(5);
        for (FutureTestThread<String> thread : threads) {
            finishedThreadsCount.acquire();
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Semaphore letOneInSemaphore = new Semaphore(0);
        Semaphore finishedThreadsCount = new Semaphore(0);

        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            letOneInSemaphore.acquire();
            finishedThreadsCount.release();
            throw error;
        });

        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 2, 3, null);

        List<FutureTestThread<String>> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));
        }

        letOneInSemaphore.release();
        finishedThreadsCount.acquire();

        FutureTestThread<String> finishedThread = getSingleFinishedThread(threads, 100L);
        assertThatThrownBy(finishedThread::await).isEqualTo(error);
        threads.remove(finishedThread);

        threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));

        letOneInSemaphore.release(5);
        for (FutureTestThread<String> thread : threads) {
            finishedThreadsCount.acquire();
            assertThatThrownBy(thread::await).isEqualTo(error);
        }
    }

    /*
     * put five elements into the queue,
     * check another one cannot be inserted
     * cancel one,
     * insert another one
     * run the tasks and check results
     */
    @Test
    public void shouldLetMaxPlus1After1Canceled() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();
        CountDownLatch invocationsStarted = new CountDownLatch(2);

        FutureTestInvocation<String> invocation = FutureTestInvocation.immediatelyReturning(() -> {
            invocationsStarted.countDown();
            delayBarrier.await();
            return completedFuture("shouldLetMaxPlus1After1Canceled");
        });

        FutureBulkhead<String> bulkhead = new FutureBulkhead<>(invocation, "shouldLetMaxPlus1After1Canceled", 2, 3, null);

        List<FutureTestThread<String>> threads = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));
        }
        invocationsStarted.countDown();
        Cancellator cancellator = new Cancellator();
        FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(cancellator, null));

        waitUntilQueueSize(bulkhead, 3, 1000);

        FutureTestThread<String> failedThread = FutureTestThread.runOnTestThread(bulkhead,
                new FutureInvocationContext<>(null, null));
        assertThatThrownBy(failedThread::await).isInstanceOf(BulkheadException.class);

        cancellator.cancel(false);

        threads.add(FutureTestThread.runOnTestThread(bulkhead, new FutureInvocationContext<>(null, null)));

        delayBarrier.open();

        for (FutureTestThread<String> thread : threads) {
            assertThat(thread.await().get()).isEqualTo("shouldLetMaxPlus1After1Canceled");
        }
    }

    private void waitUntilQueueSize(FutureBulkhead<String> bulkhead, int size, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Thread.sleep(50);
            if (bulkhead.getQueueSize() == size) {
                return;
            }
        }
        fail("queue not filled in in " + timeout + " [ms]");

    }

    private <V> FutureTestThread<V> getSingleFinishedThread(List<FutureTestThread<V>> threads,
            long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(50);
            for (FutureTestThread<V> thread : threads) {
                if (thread.isDone()) {
                    return thread;
                }
            }
        }
        fail("No thread finished in " + timeout + " ms");
        return null;
    }
}
