package io.smallrye.faulttolerance.core.retry;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CompletionStageRetryTest {

    @Test
    public void shouldNotRetryOnSuccess() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    return CompletableFuture.supplyAsync(() -> "shouldNotRetryOnSuccess");
                });
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldNotRetryOnSuccess",
                SetOfThrowables.ALL, SetOfThrowables.EMPTY,
                3, 1000L, TestDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldNotRetryOnSuccess");
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldPropagateAbortOnError() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    return CompletableFuture.supplyAsync(() -> {
                        throw error;
                    });
                });
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldPropagateAbortOnError",
                SetOfThrowables.ALL, SetOfThrowables.create(Collections.singletonList(RuntimeException.class)),
                3, 1000L, TestDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThatThrownBy(result.toCompletableFuture()::get).isInstanceOf(ExecutionException.class)
                .hasCause(error);
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    public void shouldPropagateAbortOnErrorInCSCreation() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> {
                    invocationCount.incrementAndGet();
                    throw error;
                });
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldPropagateAbortOnErrorInCSCreation",
                SetOfThrowables.ALL, SetOfThrowables.create(Collections.singletonList(RuntimeException.class)),
                3, 1000L, TestDelay.NONE, new TestStopwatch());

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
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldRetryOnce",
                SetOfThrowables.create(Collections.singletonList(RuntimeException.class)), SetOfThrowables.EMPTY,
                3, 1000L, TestDelay.NONE, new TestStopwatch());

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
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldRetryOnceOnCsFailure",
                SetOfThrowables.create(Collections.singletonList(RuntimeException.class)), SetOfThrowables.EMPTY,
                3, 1000L, TestDelay.NONE, new TestStopwatch());

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
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldRetryMaxTimesAndSucceed",
                SetOfThrowables.create(Collections.singletonList(RuntimeException.class)), SetOfThrowables.EMPTY,
                3, 1000L, TestDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThat(result.toCompletableFuture().get()).isEqualTo("shouldRetryMaxTimesAndSucceed");
        assertThat(invocationCount).hasValue(4);
    }

    @Test
    public void shouldRetryMaxTimesAndFail() throws Exception {
        RuntimeException error = new RuntimeException("forced");

        AtomicInteger invocationCount = new AtomicInteger(0);
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(
                () -> CompletableFuture.supplyAsync(() -> {
                    invocationCount.incrementAndGet();
                    throw error;
                }));
        CompletionStageRetry<String> retry = new CompletionStageRetry<>(invocation, "shouldRetryMaxTimesAndSucceed",
                SetOfThrowables.create(Collections.singletonList(RuntimeException.class)), SetOfThrowables.EMPTY,
                3, 1000L, TestDelay.NONE, new TestStopwatch());

        CompletionStage<String> result = retry.apply(new InvocationContext<>(() -> completedFuture("ignored")));
        assertThatThrownBy(result.toCompletableFuture()::get).isInstanceOf(ExecutionException.class)
                .hasCause(error);
        assertThat(invocationCount).hasValue(4);
    }

}
