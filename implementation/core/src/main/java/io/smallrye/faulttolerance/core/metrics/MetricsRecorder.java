package io.smallrye.faulttolerance.core.metrics;

import java.util.function.Supplier;

import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public interface MetricsRecorder {
    // general + fallback

    void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied);

    // retry

    void retryAttempted();

    void retryValueReturned(boolean retried);

    void retryExceptionNotRetryable(boolean retried);

    void retryMaxRetriesReached(boolean retried);

    void retryMaxDurationReached(boolean retried);

    // timeout

    void timeoutFinished(boolean timedOut, long time);

    // circuit breaker

    void circuitBreakerFinished(CircuitBreakerEvents.Result result);

    void circuitBreakerMovedToOpen();

    void registerCircuitBreakerTimeSpentInClosed(Supplier<Long> supplier);

    void registerCircuitBreakerTimeSpentInOpen(Supplier<Long> supplier);

    void registerCircuitBreakerTimeSpentInHalfOpen(Supplier<Long> supplier);

    // bulkhead

    void bulkheadDecisionMade(boolean accepted);

    void registerBulkheadExecutionsRunning(Supplier<Long> supplier);

    void registerBulkheadExecutionsWaiting(Supplier<Long> supplier);

    void updateBulkheadRunningDuration(long time);

    void updateBulkheadWaitingDuration(long time);

    // rate limit

    void rateLimitDecisionMade(boolean permitted);

    MetricsRecorder NOOP = new MetricsRecorder() {
        @Override
        public void executionFinished(boolean succeeded, boolean fallbackDefined, boolean fallbackApplied) {
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
        public void registerCircuitBreakerTimeSpentInClosed(Supplier<Long> supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInOpen(Supplier<Long> supplier) {
        }

        @Override
        public void registerCircuitBreakerTimeSpentInHalfOpen(Supplier<Long> supplier) {
        }

        @Override
        public void bulkheadDecisionMade(boolean accepted) {
        }

        @Override
        public void registerBulkheadExecutionsRunning(Supplier<Long> supplier) {
        }

        @Override
        public void registerBulkheadExecutionsWaiting(Supplier<Long> supplier) {
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
    };
}
