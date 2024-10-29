package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.Invocation.invocation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.util.TestException;

// TODO should have a test for all metrics
public class GeneralMetricsTest {
    @Test
    public void successfulInvocation() throws Throwable {
        MockMetricsRecorder metrics = new MockMetricsRecorder();

        MetricsCollector<String> collector = new MetricsCollector<>(invocation(), metrics, new MockMeteredOperation());
        assertThat(collector.apply(sync(() -> "foobar")).awaitBlocking())
                .isEqualTo("foobar");

        assertThat(metrics.valueReturned).isEqualTo(1);
        assertThat(metrics.exceptionThrown).isEqualTo(0);
    }

    @Test
    public void failingInvocation() {
        MockMetricsRecorder metrics = new MockMetricsRecorder();

        MetricsCollector<Void> collector = new MetricsCollector<>(invocation(), metrics, new MockMeteredOperation());
        assertThatThrownBy(collector.apply(sync(TestException::doThrow))::awaitBlocking)
                .isExactlyInstanceOf(TestException.class);

        assertThat(metrics.valueReturned).isEqualTo(0);
        assertThat(metrics.exceptionThrown).isEqualTo(1);
    }

    private static class MockMeteredOperation implements MeteredOperation {
        @Override
        public boolean isAsynchronous() {
            return false;
        }

        @Override
        public boolean hasBulkhead() {
            return false;
        }

        @Override
        public boolean hasCircuitBreaker() {
            return false;
        }

        @Override
        public boolean hasFallback() {
            return false;
        }

        @Override
        public boolean hasRateLimit() {
            return false;
        }

        @Override
        public boolean hasRetry() {
            return false;
        }

        @Override
        public boolean hasTimeout() {
            return false;
        }

        @Override
        public String name() {
            return "mock";
        }

        @Override
        public Object cacheKey() {
            return name();
        }
    }

    private static class MockMetricsRecorder implements MetricsRecorder {
        int valueReturned;
        int exceptionThrown;

        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
            if (succeeded) {
                valueReturned++;
            } else {
                exceptionThrown++;
            }
        }

        @Override
        public void retryAttempted() {
        }

        @Override
        public void retryValueReturned(boolean retried) {
        }

        @Override
        public void retryExceptionNotRetryable(boolean retried) {
        }

        @Override
        public void retryMaxRetriesReached(boolean retried) {
        }

        @Override
        public void retryMaxDurationReached(boolean retried) {
        }

        @Override
        public void timeoutFinished(boolean timedOut, long time) {
        }

        @Override
        public void circuitBreakerFinished(CircuitBreakerEvents.Result result) {
        }

        @Override
        public void circuitBreakerMovedToOpen() {
        }

        @Override
        public void registerCircuitBreakerIsClosed(BooleanSupplier supplier) {
        }

        @Override
        public void registerCircuitBreakerIsOpen(BooleanSupplier supplier) {
        }

        @Override
        public void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier) {
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
        }

        @Override
        public void registerBulkheadExecutionsRunning(LongSupplier supplier) {
        }

        @Override
        public void registerBulkheadExecutionsWaiting(LongSupplier supplier) {
        }

        @Override
        public void updateBulkheadRunningDuration(long time) {
        }

        @Override
        public void updateBulkheadWaitingDuration(long time) {
        }

        @Override
        public void rateLimitDecisionMade(boolean permitted) {
        }
    }
}
