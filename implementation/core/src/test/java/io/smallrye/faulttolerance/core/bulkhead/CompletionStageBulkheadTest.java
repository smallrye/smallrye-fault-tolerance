package io.smallrye.faulttolerance.core.bulkhead;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class CompletionStageBulkheadTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldLetOneIn() throws ExecutionException, InterruptedException {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("shouldLetSingleThrough"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldLetSingleThrough", 2, 2);

        CompletionStage<String> result = bulkhead.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Exception { // max threads + max queue
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("shouldLetMaxThrough"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldLetSingleThrough", 2, 3);

        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(new InvocationContext<>(null)));
        }
        for (CompletionStage<String> result : results) {
            assertThat(result.toCompletableFuture().get()).isEqualTo("shouldLetMaxThrough");
        }
    }

    @Test
    public void shouldRejectMaxPlus1() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return completedFuture("shouldRejectMaxPlus1");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldRejectMaxPlus1", 2, 3);

        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(new InvocationContext<>(null)));
        }
        // to make sure all the tasks are in bulkhead:
        waitUntilQueueSize(bulkhead, 3, 1000);

        CompletionStage<String> plus1Call = bulkhead.apply(new InvocationContext<>(null));
        assertThatThrownBy(plus1Call.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(BulkheadException.class);

        delayBarrier.open();

        for (CompletionStage<String> result : results) {
            assertThat(result.toCompletableFuture().get()).isEqualTo("shouldRejectMaxPlus1");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Left() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            return completedFuture("shouldLetMaxPlus1After1Left");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldLetMaxPlus1After1Left",
                2, 3);

        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(new InvocationContext<>(null)));
        }

        delayBarrier.open();

        CompletionStage<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThat(finishedResult.toCompletableFuture().get()).isEqualTo("shouldLetMaxPlus1After1Left");

        results.add(bulkhead.apply(new InvocationContext<>(null)));
        letOneInSemaphore.release(100);
        for (CompletionStage<String> result : results) {
            assertThat(result.toCompletableFuture().get()).isEqualTo("shouldLetMaxPlus1After1Left");
        }
    }

    @Test
    public void shouldLetMaxPlus1After1Failed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            throw error;
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldLetMaxPlus1After1Failed", 2,
                3);

        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(new InvocationContext<>(null)));
        }

        delayBarrier.open();

        CompletionStage<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThatThrownBy(finishedResult.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(error);

        results.add(bulkhead.apply(new InvocationContext<>(null)));
        letOneInSemaphore.release(100);
        for (CompletionStage<String> result : results) {
            assertThatThrownBy(result.toCompletableFuture()::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCause(error);
        }
    }

    @Test
    public void shouldLetMaxPlus1After1FailedCompletionStage() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            return CompletableFuture.supplyAsync(() -> {
                throw error;
            });
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldLetMaxPlus1After1Failed", 2,
                3);

        List<CompletionStage<String>> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(bulkhead.apply(new InvocationContext<>(null)));
        }

        delayBarrier.open();

        CompletionStage<String> finishedResult = getSingleFinishedResult(results, 1000);
        results.remove(finishedResult);
        assertThatThrownBy(finishedResult.toCompletableFuture()::get)
                .isInstanceOf(ExecutionException.class)
                .hasCause(error);

        results.add(bulkhead.apply(new InvocationContext<>(null)));
        letOneInSemaphore.release(100);
        for (CompletionStage<String> result : results) {
            assertThatThrownBy(result.toCompletableFuture()::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCause(error);
        }
    }

    @Test
    public void shouldNotStartNextIfCSInProgress() throws Exception {
        Party party = Party.create(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return CompletableFuture.supplyAsync(() -> "shouldNotStartNextIfCSInProgress");
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageBulkhead<String> bulkhead = new CompletionStageBulkhead<>(execution, "shouldNotStartNextIfCSInProgress",
                1, 1);

        CompletionStage<String> firstResult = bulkhead.apply(new InvocationContext<>(null));

        party.organizer().waitForAll();

        CompletionStage<String> secondResult = bulkhead.apply(new InvocationContext<>(null));
        waitUntilQueueSize(bulkhead, 1, 100);

        party.organizer().disband();

        assertThat(firstResult.toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(secondResult.toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(bulkhead.getQueueSize()).isEqualTo(0);
    }

    private static <V> void waitUntilQueueSize(CompletionStageBulkhead<V> bulkhead, int size, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Thread.sleep(50);
            if (bulkhead.getQueueSize() == size) {
                return;
            }
        }
        fail("queue not reached size " + size + " in " + timeout + " [ms]");
    }

    private static <V> CompletionStage<V> getSingleFinishedResult(List<CompletionStage<V>> results, long timeout)
            throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            Thread.sleep(50);
            for (CompletionStage<V> result : results) {
                if (result.toCompletableFuture().isDone()) {
                    return result;
                }
            }
        }
        throw new AssertionError("No thread finished in " + timeout + " ms");
    }
}
