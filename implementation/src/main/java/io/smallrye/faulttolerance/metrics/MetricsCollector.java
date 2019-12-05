package io.smallrye.faulttolerance.metrics;

import io.smallrye.faulttolerance.core.bulkhead.SyncBulkhead;
import io.smallrye.faulttolerance.core.circuit.breaker.SyncCircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.SyncFallback;
import io.smallrye.faulttolerance.core.retry.SyncRetry;
import io.smallrye.faulttolerance.core.timeout.SyncTimeout;

public interface MetricsCollector extends SyncRetry.MetricsRecorder, SyncFallback.MetricsRecorder,
        SyncCircuitBreaker.MetricsRecorder, SyncBulkhead.MetricsRecorder, SyncTimeout.MetricsRecorder {
    void invoked();

    void failed();
}