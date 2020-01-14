package io.smallrye.faulttolerance.metrics;

import java.util.function.Supplier;

import io.smallrye.faulttolerance.core.GeneralMetrics;
import io.smallrye.faulttolerance.core.bulkhead.SemaphoreBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.retry.Retry;
import io.smallrye.faulttolerance.core.timeout.Timeout;

public interface MetricsCollector extends GeneralMetrics, Retry.MetricsRecorder, Fallback.MetricsRecorder,
        CircuitBreaker.MetricsRecorder, SemaphoreBulkhead.MetricsRecorder, Timeout.MetricsRecorder {

    MetricsCollector NOOP = new MetricsCollector() {
        @Override
        public void invoked() {
        }

        @Override
        public void failed() {
        }

        @Override
        public void bulkheadQueueEntered() {
        }

        @Override
        public void bulkheadQueueLeft(long timeInQueue) {
        }

        @Override
        public void bulkheadEntered() {
        }

        @Override
        public void bulkheadRejected() {
        }

        @Override
        public void bulkheadLeft(long processingTime) {
        }

        @Override
        public void circuitBreakerRejected() {
        }

        @Override
        public void circuitBreakerOpenTimeProvider(Supplier<Long> supplier) {
        }

        @Override
        public void circuitBreakerHalfOpenTimeProvider(Supplier<Long> supplier) {
        }

        @Override
        public void circuitBreakerClosedTimeProvider(Supplier<Long> supplier) {
        }

        @Override
        public void circuitBreakerClosedToOpen() {
        }

        @Override
        public void circuitBreakerFailed() {
        }

        @Override
        public void circuitBreakerSucceeded() {
        }

        @Override
        public void fallbackCalled() {
        }

        @Override
        public void retrySucceededNotRetried() {
        }

        @Override
        public void retrySucceededRetried() {
        }

        @Override
        public void retryFailed() {
        }

        @Override
        public void retryRetried() {
        }

        @Override
        public void timeoutSucceeded(long time) {
        }

        @Override
        public void timeoutTimedOut(long time) {
        }

        @Override
        public void timeoutFailed(long time) {
        }
    };
}
