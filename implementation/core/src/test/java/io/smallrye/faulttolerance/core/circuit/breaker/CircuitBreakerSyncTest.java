package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.timer.TestTimer;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;

public class CircuitBreakerSyncTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(TestException.class);

    private TestStopwatch stopwatch;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();
    }

    @Test
    public void test1() throws Throwable {
        CircuitBreaker<String> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThat(cb.apply(sync(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        assertThatThrownBy(cb.apply(sync(() -> {
            throw new RuntimeException();
        }))::awaitBlocking).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(sync(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        assertThat(cb.apply(sync(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        assertThat(cb.apply(sync(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> "foobar6")).awaitBlocking()).isEqualTo("foobar6");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> "foobar7")).awaitBlocking()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> "foobar8")).awaitBlocking()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> "foobar9")).awaitBlocking()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar10")).awaitBlocking()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Throwable {
        CircuitBreaker<String> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
    }

    @Test
    public void test3() throws Throwable {
        TestTimer timer = new TestTimer();
        CircuitBreaker<String> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, timer);

        assertThat(timer.hasScheduledTasks()).isFalse();

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThat(timer.hasScheduledTasks()).isTrue();
        timer.executeSynchronously(timer.nextScheduledTask());
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        assertThat(cb.apply(sync(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
    }
}
