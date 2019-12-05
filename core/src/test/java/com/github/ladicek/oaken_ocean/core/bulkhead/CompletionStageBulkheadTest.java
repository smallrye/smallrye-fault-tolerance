package com.github.ladicek.oaken_ocean.core.bulkhead;

import static com.github.ladicek.oaken_ocean.core.bulkhead.TestInvocation.immediatelyReturning;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.TestThread;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
// TODO: do we need test threads here?
public class CompletionStageBulkheadTest {
    @Test
    public void shouldLetSingleThrough() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = immediatelyReturning(
                () -> completedFuture("shouldLetSingleThrough"));
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldLetSingleThrough", 2, 2,
                null);
        CompletionStage<String> result = bulkhead.apply(new SimpleInvocationContext<>(null));
        assertThat(result.toCompletableFuture().join()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxThrough() throws Exception { // max threads + max queue
        Barrier delayBarrier = Barrier.noninterruptible();
        TestInvocation<CompletionStage<String>> invocation = immediatelyReturning(() -> completedFuture("shouldLetMaxThrough"));
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldLetSingleThrough", 2, 3,
                null);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        for (TestThread<CompletionStage<String>> thread : threads) {
            CompletionStage<String> result = thread.await();
            assertThat(result.toCompletableFuture().join()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(delayBarrier,
                () -> completedFuture("shouldRejectMaxPlus1"));
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldRejectMaxPlus1", 2, 3,
                null);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        // to make sure all the tasks are in bulkhead:
        waitUntilQueueSize(bulkhead, 3, 1000);

        CompletionStage<String> plus1Call = bulkhead.apply(new SimpleInvocationContext<>(null));
        assertThat(plus1Call).isCompletedExceptionally();
        assertThatThrownBy(plus1Call.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(BulkheadException.class);

        delayBarrier.open();
        for (TestThread<CompletionStage<String>> thread : threads) {
            CompletionStage<String> result = thread.await();
            assertThat(result.toCompletableFuture().join()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(delayBarrier,
                () -> {
                    letOneInSemaphore.acquire();
                    return CompletableFuture.completedFuture("shouldLetMaxPlus1After1Left");
                });
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldLetMaxPlus1After1Left", 2,
                3,
                null);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        TestThread<CompletionStage<String>> finishedThread = getSingleFinishedThread(threads, 1000);
        threads.remove(finishedThread);

        assertThat(finishedThread.await().toCompletableFuture().get()).isEqualTo("shouldLetMaxPlus1After1Left");

        threads.add(TestThread.runOnTestThread(bulkhead));
        letOneInSemaphore.release(100);
        delayBarrier.open();
        for (TestThread<CompletionStage<String>> thread : threads) {
            CompletionStage<String> result = thread.await();
            assertThat(result.toCompletableFuture().join()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(delayBarrier,
                () -> {
                    letOneInSemaphore.acquire();
                    throw error;
                });
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldLetMaxPlus1After1Failed", 2,
                3,
                null);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        TestThread<CompletionStage<String>> finishedThread = getSingleFinishedThread(threads, 1000);
        threads.remove(finishedThread);

        assertThatThrownBy(finishedThread.await().toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(error);

        threads.add(TestThread.runOnTestThread(bulkhead));
        letOneInSemaphore.release(100);
        delayBarrier.open();
        for (TestThread<CompletionStage<String>> thread : threads) {
            assertThatThrownBy(thread.await().toCompletableFuture()::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCause(error);
        }
    }

    @Test
    public void shouldLetMaxPlus1After1FailedCompletionStage() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.delayed(delayBarrier,
                () -> {
                    letOneInSemaphore.acquire();
                    return CompletableFuture.supplyAsync(() -> {
                        throw error;
                    });
                });
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldLetMaxPlus1After1Failed", 2,
                3,
                null);

        List<TestThread<CompletionStage<String>>> threads = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            threads.add(TestThread.runOnTestThread(bulkhead));
        }
        delayBarrier.open();
        TestThread<CompletionStage<String>> finishedThread = getSingleFinishedThread(threads, 1000);
        threads.remove(finishedThread);

        assertThatThrownBy(finishedThread.await().toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(error);

        threads.add(TestThread.runOnTestThread(bulkhead));
        letOneInSemaphore.release(100);
        delayBarrier.open();
        for (TestThread<CompletionStage<String>> thread : threads) {
            assertThatThrownBy(thread.await().toCompletableFuture()::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCause(error);
        }
    }

    @Test
    public void shouldNotStartNextIfCSInProgress() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();
        CountDownLatch invocationStarted = new CountDownLatch(1);

        TestInvocation<CompletionStage<String>> invocation = immediatelyReturning(() -> CompletableFuture.supplyAsync(() -> {
            try {
                invocationStarted.countDown();
                delayBarrier.await();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            return "shouldNotStartNextIfCSInProgress";
        }));

        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(invocation, "shouldNotStartNextIfCSInProgress",
                1, 1, null);

        TestThread<CompletionStage<String>> firstThread = TestThread.runOnTestThread(bulkhead);

        invocationStarted.await();
        TestThread<CompletionStage<String>> secondThread = TestThread.runOnTestThread(bulkhead);
        // the execution should be put into the queue
        waitUntilQueueSize(bulkhead, 1, 100);
        // and should stay there because the first CompletionStage is not finished
        assertThatThrownBy(() -> waitUntilQueueSize(bulkhead, 0, 500)).isInstanceOf(AssertionError.class);

        delayBarrier.open();
        assertThat(firstThread.await().toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");

        assertThat(secondThread.await().toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");

        assertThat(bulkhead.getQueueSize()).isEqualTo(0);
    }

    private <V> void waitUntilQueueSize(CompletionStageBulkhead<V> bulkhead, int size, long timeout)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Thread.sleep(50);
            if (bulkhead.getQueueSize() == size) {
                return;
            }
        }
        fail("queue not reached size " + size + " in " + timeout + " [ms]");
    }

    private <V> TestThread<CompletionStage<V>> getSingleFinishedThread(List<TestThread<CompletionStage<V>>> threads,
            long timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(50);
            for (TestThread<CompletionStage<V>> thread : threads) {
                if (thread.isDone() && thread.await().toCompletableFuture().isDone()) {
                    return thread;
                }
            }
        }
        fail("No thread finished in " + timeout + " ms");
        return null;
    }
}
