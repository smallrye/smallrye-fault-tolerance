package io.smallrye.faulttolerance.core.fallback;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SyncFallback<V> extends FallbackBase<V, SimpleInvocationContext<V>> {
    public SyncFallback(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate,
            String description,
            FallbackFunction<V> fallback,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, fallback, metricsRecorder);
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        return doApply(() -> delegate.apply(target));
    }
}
