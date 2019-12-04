package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

public class SyncCircuitBreaker<V> extends CircuitBreakerBase<V, SimpleInvocationContext<V>> {

    public SyncCircuitBreaker(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate, String description,
            SetOfThrowables failOn, long delayInMillis,
            int requestVolumeThreshold, double failureRatio, int successThreshold, Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, failOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold, stopwatch,
                metricsRecorder);
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        return doApply(target);
    }
}
