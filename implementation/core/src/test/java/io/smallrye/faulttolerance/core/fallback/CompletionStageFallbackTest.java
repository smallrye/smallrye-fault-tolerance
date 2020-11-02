package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class CompletionStageFallbackTest {
    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new TestExecutor();
    }

    @Test
    public void immediatelyReturning_valueThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenDirectException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> TestException.doThrow(),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_valueThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new TestException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void immediatelyReturning_directExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void immediatelyReturning_directExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_directExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new RuntimeException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void immediatelyReturning_completionStageExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new RuntimeException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    // testing interruption and especially self-interruption isn't exactly meaningful,
    // the tests just codify existing behavior

    @Test
    public void waitingOnBarrier_interruptedInInvocation() throws Exception {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.waitingOnBarrier(startBarrier, endBarrier,
                () -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        startBarrier.await();
        executor.interruptExecutingThread();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_directException() throws Exception {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallbackFunction = ctx -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", fallbackFunction,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        startBarrier.await();
        executor.interruptExecutingThread();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void waitingOnBarrier_interruptedInFallback_completionStageException() throws Exception {
        Barrier startBarrier = Barrier.interruptible();
        Barrier endBarrier = Barrier.interruptible();
        FallbackFunction<CompletionStage<String>> fallbackFunction = ctx -> {
            startBarrier.open();
            endBarrier.await();
            return completedStage("fallback");
        };
        TestInvocation<CompletionStage<String>> invocation = TestInvocation
                .immediatelyReturning(() -> failedStage(new TestException()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", fallbackFunction,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        startBarrier.await();
        executor.interruptExecutingThread();
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_value() throws Exception {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            return completedStage("foobar");
        };
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void selfInterruptedInInvocation_directException() {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInInvocation_completionStageException() {
        FaultToleranceStrategy<CompletionStage<String>> invocation = (ignored) -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    public void selfInterruptedInFallback_value() throws Exception {
        FallbackFunction<CompletionStage<String>> fallbackFunction = ctx -> {
            Thread.currentThread().interrupt();
            return completedStage("fallback");
        };
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", fallbackFunction,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void selfInterruptedInFallback_directException() {
        FallbackFunction<CompletionStage<String>> fallbackFunction = ctx -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
        };
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", fallbackFunction,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void selfInterruptedInFallback_completionStageException() {
        FallbackFunction<CompletionStage<String>> fallbackFunction = ctx -> {
            Thread.currentThread().interrupt();
            return failedStage(new RuntimeException());
        };
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.immediatelyReturning(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", fallbackFunction,
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }
}
