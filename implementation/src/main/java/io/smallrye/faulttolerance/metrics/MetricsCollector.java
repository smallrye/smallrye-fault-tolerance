package io.smallrye.faulttolerance.metrics;

import com.github.ladicek.oaken_ocean.core.bulkhead.SyncBulkhead;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.SyncCircuitBreaker;
import com.github.ladicek.oaken_ocean.core.fallback.SyncFallback;
import com.github.ladicek.oaken_ocean.core.retry.SyncRetry;
import com.github.ladicek.oaken_ocean.core.timeout.SyncTimeout;

public interface MetricsCollector extends SyncRetry.MetricsRecorder, SyncFallback.MetricsRecorder,
        SyncCircuitBreaker.MetricsRecorder, SyncBulkhead.MetricsRecorder, SyncTimeout.MetricsRecorder {
    void invoked();

    void failed();
}