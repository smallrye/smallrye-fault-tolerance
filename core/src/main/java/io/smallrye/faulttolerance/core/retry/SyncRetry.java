package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

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

    @Override
    public V apply(SimpleInvocationContext<V> context) throws Exception {
        return doApply(context);
    }
}
