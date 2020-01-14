package io.smallrye.faulttolerance.core.composition;

import static io.smallrye.faulttolerance.core.composition.Strategies.circuitBreaker;
import static io.smallrye.faulttolerance.core.composition.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerListener;
import io.smallrye.faulttolerance.core.retry.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestException;

public class CircuitBreakerAndRetryTest {
    @Test
    public void shouldRecordEveryAttemptInCircuitBreaker() throws Exception {
        CircuitBreakerRecorder recorder = new CircuitBreakerRecorder();

        // fail 2x and then succeed, under circuit breaker
        // circuit breaker volume threshold = 5, so CB will stay closed
        FaultToleranceStrategy<String> operation = retry(
                circuitBreaker(
                        TestInvocation.initiallyFailing(2, TestException::new, () -> "foobar"),
                        recorder));

        assertThat(operation.apply(null)).isEqualTo("foobar");
        assertThat(recorder.failureCount.get()).isEqualTo(2);
        assertThat(recorder.successCount.get()).isEqualTo(1);
    }

    @Test
    public void shouldRetryCircuitBreakerInHalfOpen() throws Exception {
        CircuitBreakerRecorder recorder = new CircuitBreakerRecorder();

        // fail 5x and then succeed
        // CB volume threshold = 5, so the CB will open right after all the failures and before the success
        // CB delay is 0, so with the successful attempt, the CB will immediately move to half-open and succeed
        // doing 6 attemps (1 initial + 5 retries)
        FaultToleranceStrategy<String> operation = retry(
                circuitBreaker(
                        TestInvocation.initiallyFailing(5, TestException::new, () -> "foobar"),
                        recorder));

        assertThat(operation.apply(null)).isEqualTo("foobar");
        assertThat(recorder.failureCount.get()).isEqualTo(5);
        assertThat(recorder.successCount.get()).isEqualTo(1);
    }

    @Test
    public void shouldRetryCircuitBreakerInHalfOpenOrOpenAndFail() {
        CircuitBreakerRecorder recorder = new CircuitBreakerRecorder();

        // fail 5x and then succeed
        // CB volume threshold = 5, so the CB will open right after all the failures and before the success
        // CB delay is > 0, so the successful attempt will be prevented, because the CB will be open
        // doing 11 attemps (1 initial + 10 retries, because max retries = 10)
        FaultToleranceStrategy<String> operation = retry(
                circuitBreaker(
                        TestInvocation.initiallyFailing(5, TestException::new, () -> "foobar"),
                        100, recorder));

        assertThatThrownBy(() -> operation.apply(null)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
        assertThat(recorder.failureCount.get()).isEqualTo(5);
        assertThat(recorder.rejectedCount.get()).isEqualTo(6);
        assertThat(recorder.successCount.get()).isEqualTo(0);
    }

    private static class CircuitBreakerRecorder implements CircuitBreakerListener {
        final AtomicInteger failureCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final AtomicInteger rejectedCount = new AtomicInteger();

        @Override
        public void succeeded() {
            successCount.incrementAndGet();
        }

        @Override
        public void failed() {
            failureCount.incrementAndGet();
        }

        @Override
        public void rejected() {
            rejectedCount.incrementAndGet();
        }
    }
}
