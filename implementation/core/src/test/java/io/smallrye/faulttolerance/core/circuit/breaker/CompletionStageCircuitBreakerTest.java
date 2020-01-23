package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.InvocationContext;
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
                testException, SetOfThrowables.EMPTY,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(returning("foobar1")).toCompletableFuture().get()).isEqualTo("foobar1");
        assertThat(cb.apply(returning("foobar2")).toCompletableFuture().get()).isEqualTo("foobar2");
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new RuntimeException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(returning("foobar3")).toCompletableFuture().get()).isEqualTo("foobar3");
        assertThat(cb.apply(returning("foobar4")).toCompletableFuture().get()).isEqualTo("foobar4");
        assertThat(cb.apply(returning("foobar5")).toCompletableFuture().get()).isEqualTo("foobar5");
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new TestException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThat(cb.apply(returning("foobar6")).toCompletableFuture().get()).isEqualTo("foobar6");
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new TestException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        // circuit breaker is open
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(returning("foobar7")).toCompletableFuture().get()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new TestException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        // circuit breaker is open
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(returning("foobar8")).toCompletableFuture().get()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.apply(returning("foobar9")).toCompletableFuture().get()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.apply(returning("foobar10")).toCompletableFuture().get()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Exception {
        CompletionStageCircuitBreaker<String> cb = new CompletionStageCircuitBreaker<>(invocation(), "test invocation",
                testException, SetOfThrowables.EMPTY,
                1000, 4, 0.5, 2, stopwatch, null);

        // circuit breaker is closed
        assertThat(cb.apply(returning("foobar1")).toCompletableFuture().get()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new TestException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(immediatelyFailingWith(new TestException())).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(TestException.class);
        assertThat(cb.apply(returning("foobar2")).toCompletableFuture().get()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(returning("ignored")).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(returning("foobar3")).toCompletableFuture().get()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.apply(returning("foobar4")).toCompletableFuture().get()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.apply(returning("foobar5")).toCompletableFuture().get()).isEqualTo("foobar5");
    }

    @Test
    public void shouldTreatCompletionStageFailureAsCBFailure() throws Exception {
        TestException exception = new TestException();

        CompletionStageCircuitBreaker<String> cb = new CompletionStageCircuitBreaker<>(invocation(), "test invocation",
                testException, SetOfThrowables.EMPTY,
                1000, 4, 0.5, 2, stopwatch, null);

        assertThatThrownBy(cb.apply(eventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(eventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(eventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);
        assertThatThrownBy(cb.apply(eventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCause(exception);

        assertThatThrownBy(cb.apply(eventuallyFailingWith(exception)).toCompletableFuture()::get)
                .isExactlyInstanceOf(ExecutionException.class).hasCauseInstanceOf(CircuitBreakerOpenException.class);
    }

    private static InvocationContext<CompletionStage<String>> returning(String value) {
        return new InvocationContext<>(() -> completedStage(value));
    }

    private static <V> InvocationContext<CompletionStage<V>> immediatelyFailingWith(Exception exception) {
        return new InvocationContext<>(() -> {
            throw exception;
        });
    }

    private static <V> InvocationContext<CompletionStage<V>> eventuallyFailingWith(Exception exception) {
        return new InvocationContext<>(() -> failedStage(exception));
    }
}
