package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CompletionStageExecution;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.TestInvocation;

public class CompletionStageFallbackTest {
    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new TestExecutor();
    }

    @Test
    public void allExceptionsSupported_valueThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_valueThenDirectException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> TestException.doThrow(),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_valueThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new TestException()),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_directExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void allExceptionsSupported_completionStageExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("fallback");
    }

    @Test
    public void allExceptionsSupported_directExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(TestException::doThrow);
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
    public void allExceptionsSupported_directExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(TestException::doThrow);
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
    public void allExceptionsSupported_completionStageExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
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
    public void allExceptionsSupported_completionStageExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
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
    public void noExceptionSupported_valueThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_valueThenDirectException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> TestException.doThrow(),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_valueThenCompletionStageException() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> completedStage("foobar"));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new TestException()),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThat(result.toCompletableFuture().get()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_directExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(TestException::doThrow);
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenValue() throws Exception {
        TestInvocation<CompletionStage<String>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
        CompletionStageExecution<String> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<String> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> completedStage("fallback"),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<String> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_directExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(TestException::doThrow);
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_directExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(TestException::doThrow);
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new RuntimeException()),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenDirectException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenCompletionStageException() {
        TestInvocation<CompletionStage<Void>> invocation = TestInvocation.of(() -> failedStage(new TestException()));
        CompletionStageExecution<Void> execution = new CompletionStageExecution<>(invocation, executor);
        CompletionStageFallback<Void> fallback = new CompletionStageFallback<>(execution,
                "test invocation", ctx -> failedStage(new RuntimeException()),
                SetOfThrowables.EMPTY, SetOfThrowables.ALL);
        CompletionStage<Void> result = fallback.apply(new InvocationContext<>(null));
        assertThatThrownBy(result.toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }
}
