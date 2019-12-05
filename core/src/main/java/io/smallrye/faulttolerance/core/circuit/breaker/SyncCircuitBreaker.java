package io.smallrye.faulttolerance.core.circuit.breaker;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

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
