package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.timer.TestTimer;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;

public class CircuitBreakerAsyncTest {
    private static final SetOfThrowables testException = SetOfThrowables.create(TestException.class);

    private TestStopwatch stopwatch;

    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        stopwatch = new TestStopwatch();

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void test1() throws Throwable {
        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        CircuitBreaker<String> cb = new CircuitBreaker<>(execution, "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThat(cb.apply(async(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        assertThatThrownBy(cb.apply(async(() -> {
            throw new RuntimeException();
        }))::awaitBlocking)
                .isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(async(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        assertThat(cb.apply(async(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        assertThat(cb.apply(async(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(async(() -> "foobar6")).awaitBlocking()).isEqualTo("foobar6");
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(async(() -> "foobar7")).awaitBlocking()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(async(() -> "foobar8")).awaitBlocking()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(async(() -> "foobar9")).awaitBlocking()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar10")).awaitBlocking()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Throwable {
        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        CircuitBreaker<String> cb = new CircuitBreaker<>(execution, "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(async(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(async(() -> "ignored"))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(async(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(async(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
    }

    @Test
    public void test3() throws Throwable {
        TestTimer timer = new TestTimer();
        ThreadOffload<String> execution = new ThreadOffload<>(invocation(), executor);
        CircuitBreaker<String> cb = new CircuitBreaker<>(execution, "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, timer);

        assertThat(timer.hasScheduledTasks()).isFalse();

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar1")).awaitBlocking()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(async(TestException::doThrow))::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(async(() -> "foobar2")).awaitBlocking()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThat(timer.hasScheduledTasks()).isTrue();
        timer.executeSynchronously(timer.nextScheduledTask());
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(async(() -> "foobar3")).awaitBlocking()).isEqualTo("foobar3");
        assertThat(cb.apply(async(() -> "foobar4")).awaitBlocking()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(async(() -> "foobar5")).awaitBlocking()).isEqualTo("foobar5");
    }
}
