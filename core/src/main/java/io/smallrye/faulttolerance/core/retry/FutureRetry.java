package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

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
