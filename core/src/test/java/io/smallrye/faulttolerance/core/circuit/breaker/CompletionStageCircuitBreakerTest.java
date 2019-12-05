package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.SimpleInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CompletionStageCircuitBreakerTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(Collections.singletonList(TestException.class));

    private TestStopwatch stopwatch;

    @Before
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Exception {
        CompletionStageCircuitBreaker<String> cb = new CompletionStageCircuitBreaker<>(invocation(), "test invocation",
                testException,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(contextReturning("foobar1")).toCompletableFuture().get()).isEqualTo("foobar1");
        assertThat(cb.apply(contextReturning("foobar2")).toCompletableFuture().get()).isEqualTo("foobar2");
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(() -> {
            throw new RuntimeException();
        })).toCompletableFuture()::get).isExactlyInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(contextReturning("foobar3")).toCompletableFuture().get()).isEqualTo("foobar3");
        assertThat(cb.apply(contextReturning("foobar4")).toCompletableFuture().get()).isEqualTo("foobar4");
        assertThat(cb.apply(contextReturning("foobar5")).toCompletableFuture().get()).isEqualTo("foobar5");
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(TestException::doThrow)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThat(cb.apply(contextReturning("foobar6")).toCompletableFuture().get()).isEqualTo("foobar6");
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(TestException::doThrow)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(contextReturning("foobar7")).toCompletableFuture().get()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(TestException::doThrow)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(contextReturning("foobar8")).toCompletableFuture().get()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.apply(contextReturning("foobar9")).toCompletableFuture().get()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.apply(contextReturning("foobar10")).toCompletableFuture().get()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        CompletionStageCircuitBreaker<String> cb = new CompletionStageCircuitBreaker<>(invocation(), "test invocation",
                testException,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(contextReturning("foobar1")).toCompletableFuture().get()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(TestException::doThrow)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(new SimpleInvocationContext<>(TestException::doThrow)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThat(cb.apply(contextReturning("foobar2")).toCompletableFuture().get()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(contextReturning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(contextReturning("foobar3")).toCompletableFuture().get()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.apply(contextReturning("foobar4")).toCompletableFuture().get()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.apply(contextReturning("foobar5")).toCompletableFuture().get()).isEqualTo("foobar5");
    }

    @Test
    public void shouldTreatCompletionStageFailureAsCBFailure() throws Exception {
        TestException exception = new TestException();

        CompletionStageCircuitBreaker<String> cb = new CompletionStageCircuitBreaker<>(invocation(), "test invocation",
                testException,
                1000, 4, 0.5, 2, stopwatch, null);

        assertThatThrownBy(cb.apply(contextEventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(contextEventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(contextEventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(contextEventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);

        assertThatThrownBy(cb.apply(contextEventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
    }

    private SimpleInvocationContext<CompletionStage<String>> contextReturning(String value) {
        return new SimpleInvocationContext<>(() -> CompletableFuture.completedFuture(value));
    }

    private <V> SimpleInvocationContext<CompletionStage<V>> contextEventuallyFailingWith(Exception exception) {
        return new SimpleInvocationContext<>(() -> {
            CompletableFuture<V> result = new CompletableFuture<>();
            result.completeExceptionally(exception);
            return result;
        });
    }
}
