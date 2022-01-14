package io.smallrye.faulttolerance.core.retry;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageRetryTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldNotRetryOnSuccess() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    return CompletableFuture.supplyAsync(() -> "shouldNotRetryOnSuccess");
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldNotRetryOnSuccess",
                ExceptionDecision.ALWAYS_FAILURE, 3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldNotRetryOnSuccess");
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldPropagateAbortOnError() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    return CompletableFuture.supplyAsync(() -> {
                        throw error;
                    });
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldPropagateAbortOnError",
                new ExceptionDecision(SetOfThrowables.ALL, SetOfThrowables.create(RuntimeException.class), false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThatThrownBy(result.toCompletableFuture()::get).isInstanceOf(ExecutionException.class)
                .hasCause(error);
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldPropagateAbortOnErrorInCSCreation() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    throw error;
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldPropagateAbortOnErrorInCSCreation",
                new ExceptionDecision(SetOfThrowables.ALL, SetOfThrowables.create(RuntimeException.class), false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThatThrownBy(result.toCompletableFuture()::get).isInstanceOf(ExecutionException.class)
                .hasCause(error);
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldRetryOnce() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    int prevInvoCnt = invocationCount.getAndIncrement();
                    if (prevInvoCnt == 0) {
                        throw error;
                    } else {
                        return completedFuture("shouldRetryOnce");
                    }
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldRetryOnce",
                new ExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldRetryOnce");
        assertThat(invocationCount).hasValue(2);
    }

    @Test
    public void shouldRetryOnceOnCsFailure() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    int prevInvoCnt = invocationCount.getAndIncrement();
                    return CompletableFuture.supplyAsync(() -> {
                        if (prevInvoCnt == 0) {
                            throw error;
                        } else {
                            return "shouldRetryOnceOnCsFailure";
                        }
                    });
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldRetryOnceOnCsFailure",
                new ExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldRetryOnceOnCsFailure");
        assertThat(invocationCount).hasValue(2);
    }

    @Test
    public void shouldRetryMaxTimesAndSucceed() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    int prevInvoCnt = invocationCount.getAndIncrement();
                    return CompletableFuture.supplyAsync(() -> {
                        if (prevInvoCnt < 3) {
                            throw error;
                        } else {
                            return "shouldRetryMaxTimesAndSucceed";
                        }
                    });
                });
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldRetryMaxTimesAndSucceed",
                new ExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldRetryMaxTimesAndSucceed");
        assertThat(invocationCount).hasValue(4);
    }

    @Test
    public void shouldRetryMaxTimesAndFail() {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> CompletableFuture.supplyAsync(() -> {
                    invocationCount.incrementAndGet();
                    throw error;
                }));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(execution, "shouldRetryMaxTimesAndSucceed",
                new ExceptionDecision(SetOfThrowables.create(RuntimeException.class), SetOfThrowables.EMPTY, false),
                3, 1000L, AsyncDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThatThrownBy(result.toCompletableFuture()::get).isInstanceOf(ExecutionException.class)
                .hasCause(error);
        assertThat(invocationCount).hasValue(4);
    }

}
