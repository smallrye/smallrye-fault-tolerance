package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.timer.TestTimer;
import io.smallrye.faulttolerance.core.util.SetBasedExceptionDecision;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;
import io.smallrye.faulttolerance.core.util.TestException;

public class CircuitBreakerFutureTest {
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
        CircuitBreaker<Future<String>> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());
        //FutureExecution<String> cb = new FutureExecution<>(cb, executor);

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar1"))).awaitBlocking().get()).isEqualTo("foobar1");
        assertThat(cb.apply(sync(() -> completedFuture("foobar2"))).awaitBlocking().get()).isEqualTo("foobar2");
        assertThatThrownBy(cb.apply(sync(() -> {
            throw new RuntimeException();
        }))::awaitBlocking).isExactlyInstanceOf(RuntimeException.class); // treated as success
        assertThat(cb.apply(sync(() -> completedFuture("foobar3"))).awaitBlocking().get()).isEqualTo("foobar3");
        assertThat(cb.apply(sync(() -> completedFuture("foobar4"))).awaitBlocking().get()).isEqualTo("foobar4");
        assertThat(cb.apply(sync(() -> completedFuture("foobar5"))).awaitBlocking().get()).isEqualTo("foobar5");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> completedFuture("foobar6"))).awaitBlocking().get()).isEqualTo("foobar6");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> completedFuture("foobar7"))).awaitBlocking().get()).isEqualTo("foobar7");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        stopwatch.setCurrentValue(0);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> completedFuture("foobar8"))).awaitBlocking().get()).isEqualTo("foobar8");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> completedFuture("foobar9"))).awaitBlocking().get()).isEqualTo("foobar9");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar10"))).awaitBlocking().get()).isEqualTo("foobar10");
    }

    @Test
    public void test2() throws Throwable {
        CircuitBreaker<Future<String>> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, new TestTimer());

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar1"))).awaitBlocking().get()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> completedFuture("foobar2"))).awaitBlocking().get()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThatThrownBy(cb.apply(sync(() -> completedFuture("ignored")))::awaitBlocking)
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
        stopwatch.setCurrentValue(1500);
        assertThat(cb.apply(sync(() -> completedFuture("foobar3"))).awaitBlocking().get()).isEqualTo("foobar3");
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> completedFuture("foobar4"))).awaitBlocking().get()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar5"))).awaitBlocking().get()).isEqualTo("foobar5");
    }

    @Test
    public void test3() throws Throwable {
        TestTimer timer = new TestTimer();
        CircuitBreaker<Future<String>> cb = new CircuitBreaker<>(invocation(), "test invocation",
                new SetBasedExceptionDecision(testException, SetOfThrowables.EMPTY, false),
                1000, 4, 0.5, 2, stopwatch, timer);

        assertThat(timer.hasScheduledTasks()).isFalse();

        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar1"))).awaitBlocking().get()).isEqualTo("foobar1");
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        assertThatThrownBy(cb.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);
        assertThat(cb.apply(sync(() -> completedFuture("foobar2"))).awaitBlocking().get()).isEqualTo("foobar2");
        // circuit breaker is open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_OPEN);
        assertThat(timer.hasScheduledTasks()).isTrue();
        timer.executeSynchronously(timer.nextScheduledTask());
        // circuit breaker is half-open
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_HALF_OPEN);
        assertThat(cb.apply(sync(() -> completedFuture("foobar3"))).awaitBlocking().get()).isEqualTo("foobar3");
        assertThat(cb.apply(sync(() -> completedFuture("foobar4"))).awaitBlocking().get()).isEqualTo("foobar4");
        // circuit breaker is closed
        assertThat(cb.currentState()).isEqualTo(CircuitBreaker.STATE_CLOSED);
        assertThat(cb.apply(sync(() -> completedFuture("foobar5"))).awaitBlocking().get()).isEqualTo("foobar5");
    }
}
