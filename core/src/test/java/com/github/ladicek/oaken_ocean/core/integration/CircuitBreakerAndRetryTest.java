package com.github.ladicek.oaken_ocean.core.integration;

import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreakerListener;
import com.github.ladicek.oaken_ocean.core.retry.TestAction;
import com.github.ladicek.oaken_ocean.core.util.TestException;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ladicek.oaken_ocean.core.integration.Strategies.circuitBreaker;
import static com.github.ladicek.oaken_ocean.core.integration.Strategies.retry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CircuitBreakerAndRetryTest {
    @Test
    public void shouldRecordEveryAttemptInCircuitBreaker() throws Exception {
        CircuitBreakerRecorder recorder = new CircuitBreakerRecorder();

        // fail 2x and then succeed, under circuit breaker
        // circuit breaker volume threshold = 5, so CB will stay closed
        Callable<String> operation =
                retry(
                        circuitBreaker(
                                TestAction.initiallyFailing(2, TestException::new, () -> "foobar"),
                                recorder
                        )
                );

        assertThat(operation.call()).isEqualTo("foobar");
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
        Callable<String> operation =
                retry(
                        circuitBreaker(
                                TestAction.initiallyFailing(5, TestException::new, () -> "foobar"),
                                recorder
                        )
                );

        assertThat(operation.call()).isEqualTo("foobar");
        assertThat(recorder.failureCount.get()).isEqualTo(5);
        assertThat(recorder.successCount.get()).isEqualTo(1);
    }

    @Test
    public void shouldRetryCircuitBreakerInHalfOpenOrOpenAndFail() throws Exception {
        CircuitBreakerRecorder recorder = new CircuitBreakerRecorder();

        // fail 5x and then succeed
        // CB volume threshold = 5, so the CB will open right after all the failures and before the success
        // CB delay is > 0, so the successful attempt will be prevented, because the CB will be open
        // doing 11 attemps (1 initial + 10 retries, because max retries = 10)
        Callable<String> operation =
                retry(
                        circuitBreaker(
                                TestAction.initiallyFailing(5, TestException::new, () -> "foobar"),
                                100, recorder
                        )
                );

        assertThatThrownBy(operation::call).hasMessage("retry reached max retries or max retry duration");
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
