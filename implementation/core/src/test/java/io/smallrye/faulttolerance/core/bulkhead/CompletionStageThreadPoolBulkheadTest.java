package io.smallrye.faulttolerance.core.bulkhead;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class CompletionStageThreadPoolBulkheadTest {
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
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldLetSingleThrough", 2, 2);

        CompletionStage<String> result = bulkhead.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldLetSingleThrough");
    }

    @Test
    public void shouldLetMaxIn() throws Exception { // max threads + max queue
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedFuture("shouldLetMaxThrough"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldLetSingleThrough", 2, 3);

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
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldRejectMaxPlus1", 2, 3);

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
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldLetMaxPlus1After1Left", 2, 3);

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
    public void shouldLetMaxPlus1After1Failed() {
        RuntimeException error = new RuntimeException("forced");

        Barrier delayBarrier = Barrier.noninterruptible();
        Semaphore letOneInSemaphore = new Semaphore(1);

        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            letOneInSemaphore.acquire();
            throw error;
        });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldLetMaxPlus1After1Failed", 2, 3);

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
    public void shouldLetMaxPlus1After1FailedCompletionStage() {
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
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldLetMaxPlus1After1Failed", 2, 3);

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
        CompletionStageThreadPoolBulkhead<String> bulkhead = new CompletionStageThreadPoolBulkhead<>(execution,
                "shouldNotStartNextIfCSInProgress", 1, 1);

        CompletionStage<String> firstResult = bulkhead.apply(new InvocationContext<>(null));

        party.organizer().waitForAll();

        CompletionStage<String> secondResult = bulkhead.apply(new InvocationContext<>(null));
        waitUntilQueueSize(bulkhead, 1, 150);

        party.organizer().disband();

        assertThat(firstResult.toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(secondResult.toCompletableFuture().get()).isEqualTo("shouldNotStartNextIfCSInProgress");
        assertThat(bulkhead.getQueueSize()).isZero();
    }

    private static <V> void waitUntilQueueSize(CompletionStageThreadPoolBulkhead<V> bulkhead, int size, long timeout) {
        await().atMost(Duration.ofMillis(timeout)).until(() -> bulkhead.getQueueSize() == size);
    }

    private static <V> CompletionStage<V> getSingleFinishedResult(List<CompletionStage<V>> results, long timeout) {
        return await().atMost(Duration.ofMillis(timeout))
                .until(() -> getSingleFinishedResult(results), Optional::isPresent)
                .get();
    }

    private static <V> Optional<CompletionStage<V>> getSingleFinishedResult(List<CompletionStage<V>> results) {
        for (CompletionStage<V> result : results) {
            if (result.toCompletableFuture().isDone()) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }
}
