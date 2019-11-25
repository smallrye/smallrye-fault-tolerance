package io.smallrye.faulttolerance.metrics;

import com.github.ladicek.oaken_ocean.core.bulkhead.Bulkhead;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreaker;
import com.github.ladicek.oaken_ocean.core.fallback.Fallback;
import com.github.ladicek.oaken_ocean.core.retry.Retry;
import com.github.ladicek.oaken_ocean.core.timeout.Timeout;

public interface MetricsCollector extends Retry.MetricsRecorder, Fallback.MetricsRecorder,
        CircuitBreaker.MetricsRecorder, Bulkhead.MetricsRecorder, Timeout.MetricsRecorder {
    void invoked();

    void failed();
}