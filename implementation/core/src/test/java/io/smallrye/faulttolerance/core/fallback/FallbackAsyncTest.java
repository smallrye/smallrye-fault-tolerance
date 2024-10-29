package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.TestInvocation;

public class FallbackAsyncTest {
    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        executor = new TestExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdown();
    }

    @Test
    public void allExceptionsSupported_valueThenValue() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_valueThenDirectException() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution, "test invocation", ctx -> {
            throw sneakyThrow(new TestException());
        }, ExceptionDecision.ALWAYS_FAILURE);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_valueThenCompletionStageException() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new TestException()),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void allExceptionsSupported_directExceptionThenValue() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("fallback");
    }

    @Test
    public void allExceptionsSupported_completionStageExceptionThenValue() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("fallback");
    }

    @Test
    public void allExceptionsSupported_directExceptionThenDirectException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, ExceptionDecision.ALWAYS_FAILURE);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void allExceptionsSupported_directExceptionThenCompletionStageException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new RuntimeException()),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void allExceptionsSupported_completionStageExceptionThenDirectException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, ExceptionDecision.ALWAYS_FAILURE);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void allExceptionsSupported_completionStageExceptionThenCompletionStageException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new RuntimeException()),
                ExceptionDecision.ALWAYS_FAILURE);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void noExceptionSupported_valueThenValue() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_valueThenDirectException() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> {
                    throw sneakyThrow(new TestException());
                },
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_valueThenCompletionStageException() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new TestException()),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<String> result = fallback.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void noExceptionSupported_directExceptionThenValue() {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<String> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenValue() {
        TestInvocation<String> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Fallback<String> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.of("fallback"),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<String> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_directExceptionThenDirectException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, ExceptionDecision.ALWAYS_EXPECTED);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_directExceptionThenCompletionStageException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new RuntimeException()),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenDirectException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> {
                    throw new RuntimeException();
                }, ExceptionDecision.ALWAYS_EXPECTED);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void noExceptionSupported_completionStageExceptionThenCompletionStageException() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Fallback<Void> fallback = new Fallback<>(execution,
                "test invocation", ctx -> Future.ofError(new RuntimeException()),
                ExceptionDecision.ALWAYS_EXPECTED);
        Future<Void> result = fallback.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }
}
