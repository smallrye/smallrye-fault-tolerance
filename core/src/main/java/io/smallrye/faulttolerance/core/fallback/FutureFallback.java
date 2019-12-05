package io.smallrye.faulttolerance.core.fallback;

import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureFallback<V> extends FallbackBase<Future<V>, FutureInvocationContext<V>> {
    public FutureFallback(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate,
            String description,
            FallbackFunction<Future<V>> fallback,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, fallback, metricsRecorder);
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        return doApply(() -> delegate.apply(target));
    }
}
