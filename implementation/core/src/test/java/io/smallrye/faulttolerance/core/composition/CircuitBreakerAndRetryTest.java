package io.smallrye.faulttolerance.core.composition;

import static io.smallrye.faulttolerance.core.composition.Strategies.circuitBreaker;
import static io.smallrye.faulttolerance.core.composition.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.retry.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestException;

public class CircuitBreakerAndRetryTest {
    @Test
    public void shouldRecordEveryAttemptInCircuitBreaker() throws Exception {
        // fail 2x and then succeed, under circuit breaker
        // circuit breaker volume threshold = 5, so CB will stay closed
        CircuitBreakerRecorder<String> operation = new CircuitBreakerRecorder<>(
                retry(
                        circuitBreaker(
                                TestInvocation.initiallyFailing(2, TestException::new, () -> "foobar"))));

        assertThat(operation.apply(new InvocationContext<>(() -> "ignored"))).isEqualTo("foobar");

        assertThat(operation.successCount.get()).isEqualTo(1);
        assertThat(operation.failureCount.get()).isEqualTo(2);
    }

    @Test
    public void shouldRetryCircuitBreakerInHalfOpen() throws Exception {
        // fail 5x and then succeed
        // CB volume threshold = 5, so the CB will open right after all the failures and before the success
        // CB delay is 0, so with the successful attempt, the CB will immediately move to half-open and succeed
        // doing 6 attemps (1 initial + 5 retries)
        CircuitBreakerRecorder<String> operation = new CircuitBreakerRecorder<>(
                retry(
                        circuitBreaker(
                                TestInvocation.initiallyFailing(5, TestException::new, () -> "foobar"))));

        assertThat(operation.apply(new InvocationContext<>(() -> "ignored"))).isEqualTo("foobar");

        assertThat(operation.successCount.get()).isEqualTo(1);
        assertThat(operation.failureCount.get()).isEqualTo(5);
    }

    @Test
    public void shouldRetryCircuitBreakerInHalfOpenOrOpenAndFail() {
        // fail 5x and then succeed
        // CB volume threshold = 5, so the CB will open right after all the failures and before the success
        // CB delay is > 0, so the successful attempt will be prevented, because the CB will be open
        // doing 11 attemps (1 initial + 10 retries, because max retries = 10)
        CircuitBreakerRecorder<String> operation = new CircuitBreakerRecorder<>(
                retry(
                        circuitBreaker(
                                TestInvocation.initiallyFailing(5, TestException::new, () -> "foobar"),
                                100)));

        assertThatThrownBy(() -> operation.apply(new InvocationContext<>(() -> "ignored")))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);

        assertThat(operation.successCount.get()).isEqualTo(0);
        assertThat(operation.failureCount.get()).isEqualTo(5);
        assertThat(operation.preventedCount.get()).isEqualTo(6);
    }

    private static class CircuitBreakerRecorder<V> implements FaultToleranceStrategy<V> {
        private final FaultToleranceStrategy<V> delegate;

        final AtomicInteger successCount = new AtomicInteger();
        final AtomicInteger failureCount = new AtomicInteger();
        final AtomicInteger preventedCount = new AtomicInteger();

        CircuitBreakerRecorder(FaultToleranceStrategy<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public V apply(InvocationContext<V> ctx) throws Exception {
            ctx.registerEventHandler(CircuitBreakerEvents.Finished.class, event -> {
                switch (event.result) {
                    case SUCCESS:
                        successCount.incrementAndGet();
                        break;
                    case FAILURE:
                        failureCount.incrementAndGet();
                        break;
                    case PREVENTED:
                        preventedCount.incrementAndGet();
                        break;
                }
            });
            return delegate.apply(ctx);
        }
    }
}
