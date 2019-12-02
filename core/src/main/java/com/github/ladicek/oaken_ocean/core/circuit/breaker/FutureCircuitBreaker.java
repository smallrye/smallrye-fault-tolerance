package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

import java.util.concurrent.Future;

public class FutureCircuitBreaker<V> extends CircuitBreakerBase<Future<V>, FutureInvocationContext<V>> {


    public FutureCircuitBreaker(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate, String description, SetOfThrowables failOn, long delayInMillis,
                                int requestVolumeThreshold, double failureRatio, int successThreshold, Stopwatch stopwatch,
                                MetricsRecorder metricsRecorder) {
        super(delegate, description, failOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold, stopwatch, metricsRecorder);
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        return doApply(target);
    }
}
