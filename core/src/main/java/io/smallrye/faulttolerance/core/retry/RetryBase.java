package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public abstract class RetryBase<V, ContextType extends InvocationContext<V>> implements FaultToleranceStrategy<V, ContextType> {
    final FaultToleranceStrategy<V, ContextType> delegate;
    final String description;

    final SetOfThrowables retryOn;
    final SetOfThrowables abortOn;
    final long maxRetries; // this is an `int` in MP FT, but `long` allows easier handling of "infinity"
    final long maxTotalDurationInMillis;
    final Delay delayBetweenRetries;
    final Stopwatch stopwatch;
    final MetricsRecorder metricsRecorder;

    // mstodo reattempt should not be triggered before the previous one. So we should be good
    // mstodo move to the call class if we were to separate from the context class

    public RetryBase(FaultToleranceStrategy<V, ContextType> delegate, String description, SetOfThrowables retryOn,
            SetOfThrowables abortOn,
            long maxRetries, long maxTotalDurationInMillis, Delay delayBetweenRetries, Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Retry delegate must be set");
        this.description = checkNotNull(description, "Retry description must be set");
        this.retryOn = checkNotNull(retryOn, "Set of retry-on throwables must be set");
        this.abortOn = checkNotNull(abortOn, "Set of abort-on throwables must be set");
        this.maxRetries = maxRetries < 0 ? Long.MAX_VALUE : maxRetries;
        this.maxTotalDurationInMillis = maxTotalDurationInMillis <= 0 ? Long.MAX_VALUE : maxTotalDurationInMillis;
        this.delayBetweenRetries = checkNotNull(delayBetweenRetries, "Delay must be set");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
    }

    V doApply(ContextType context) throws Exception {
        long counter = 0;
        RunningStopwatch runningStopwatch = stopwatch.start();
        Throwable lastFailure = null;
        while (counter <= maxRetries && runningStopwatch.elapsedTimeInMillis() < maxTotalDurationInMillis) {
            if (counter > 0) {
                metricsRecorder.retryRetried();
            }
            try {
                V result = delegate.apply(context);
                if (counter == 0) {
                    metricsRecorder.retrySucceededNotRetried();
                } else {
                    metricsRecorder.retrySucceededRetried();
                }
                return result;
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                if (Thread.interrupted()) {
                    metricsRecorder.retryFailed();
                    throw new InterruptedException();
                }

                // specifying `abortOn` is only useful when it's more specific than `retryOn`;
                // otherwise, if the exception isn't present in `retryOn`, it's always an abort
                if (shouldAbortRetrying(e)) {
                    metricsRecorder.retryFailed();
                    throw e;
                }

                lastFailure = e;
            }

            try {
                delayBetweenRetries.sleep();
            } catch (InterruptedException e) {
                metricsRecorder.retryFailed();
                throw e;
            } catch (Exception e) {
                metricsRecorder.retryFailed();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                throw e;
            }

            counter++;
        }

        metricsRecorder.retryFailed();
        if (lastFailure != null) {

            if (lastFailure instanceof Exception) {
                throw (Exception) lastFailure;
            } else {
                throw new FaultToleranceException(lastFailure.getMessage(), lastFailure);
            }
        } else {
            throw new FaultToleranceException(description + " reached max retries or max retry duration");
        }
    }

    boolean shouldAbortRetrying(Throwable e) {
        return abortOn.includes(e.getClass()) || !retryOn.includes(e.getClass());
    }

    public interface MetricsRecorder {
        void retrySucceededNotRetried();

        void retrySucceededRetried();

        void retryFailed();

        void retryRetried();

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void retrySucceededNotRetried() {
            }

            @Override
            public void retrySucceededRetried() {
            }

            @Override
            public void retryFailed() {
            }

            @Override
            public void retryRetried() {
            }
        };
    }
}
