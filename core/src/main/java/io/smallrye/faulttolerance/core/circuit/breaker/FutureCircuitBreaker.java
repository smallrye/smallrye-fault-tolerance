package io.smallrye.faulttolerance.core.circuit.breaker;

import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class FutureCircuitBreaker<V> extends CircuitBreakerBase<Future<V>, FutureInvocationContext<V>> {

    public FutureCircuitBreaker(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate, String description,
            SetOfThrowables failOn, long delayInMillis,
            int requestVolumeThreshold, double failureRatio, int successThreshold, Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, failOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold, stopwatch,
                metricsRecorder);
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        return doApply(target);
    }
}
