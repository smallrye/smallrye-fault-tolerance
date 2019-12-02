package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;

import java.util.concurrent.Future;

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
