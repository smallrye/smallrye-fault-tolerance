package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

public class SyncRetry<V> extends RetryBase<V, SimpleInvocationContext<V>> {
    public SyncRetry(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate,
            String description,
            SetOfThrowables retryOn,
            SetOfThrowables abortOn,
            long maxRetries, long maxTotalDurationInMillis,
            Delay delayBetweenRetries,
            Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch,
                metricsRecorder);
    }

    // mstodo reattempt should not be triggered before the previous one. So we should be good
    // mstodo move to the call class if we were to separate from the context class

    @Override
    public V apply(SimpleInvocationContext<V> context) throws Exception {
        return doApply(context);
    }
}
