package com.github.ladicek.oaken_ocean.core.retry;

import java.util.concurrent.Future;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureRetry<V> extends RetryBase<Future<V>, FutureInvocationContext<V>> {
    public FutureRetry(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate,
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
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        return doApply(target);
    }
}
