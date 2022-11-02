package io.smallrye.faulttolerance.core.metrics;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

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

    void registerCircuitBreakerIsClosed(BooleanSupplier supplier);

    void registerCircuitBreakerIsOpen(BooleanSupplier supplier);

    void registerCircuitBreakerIsHalfOpen(BooleanSupplier supplier);

    void registerCircuitBreakerTimeSpentInClosed(LongSupplier supplier);

    void registerCircuitBreakerTimeSpentInOpen(LongSupplier supplier);

    void registerCircuitBreakerTimeSpentInHalfOpen(LongSupplier supplier);

    // bulkhead

    void bulkheadDecisionMade(boolean accepted);

    void registerBulkheadExecutionsRunning(LongSupplier supplier);

    void registerBulkheadExecutionsWaiting(LongSupplier supplier);

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
    };
}
