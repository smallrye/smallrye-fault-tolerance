package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageRetry<V> extends Retry<CompletionStage<V>> {
    public CompletionStageRetry(
            FaultToleranceStrategy<CompletionStage<V>> delegate,
            String description,
            SetOfThrowables retryOn,
            SetOfThrowables abortOn,
            long maxRetries, long maxTotalDurationInMillis,
            Delay delayBetweenRetries, Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch,
                metricsRecorder);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        RunningStopwatch runningStopwatch = stopwatch.start();
        return doRetry(ctx, 0, runningStopwatch, null);
    }

    public CompletionStage<V> doRetry(InvocationContext<CompletionStage<V>> target, int attempt,
            RunningStopwatch stopwatch, Throwable latestFailure) {
        if (attempt == 0) {
            // do not sleep
        } else if (attempt <= maxRetries) {
            metricsRecorder.retryRetried();

            try {
                delayBetweenRetries.sleep();
            } catch (InterruptedException e) {
                metricsRecorder.retryFailed();
                return failedStage(e);
            } catch (Exception e) {
                metricsRecorder.retryFailed();
                if (Thread.interrupted()) {
                    return failedStage(new InterruptedException());
                }

                return failedStage(e);
            }
        } else {
            metricsRecorder.retryFailed();
            return failedStage(latestFailure);
        }
        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            if (latestFailure != null) {
                metricsRecorder.retryFailed();
                return failedStage(latestFailure);
            } else {
                metricsRecorder.retryFailed();
                return failedStage(new FaultToleranceException(description + " reached max retries or max retry duration"));
            }
        }
        try {
            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(target).whenComplete((value, exception) -> {
                if (exception == null) {
                    if (attempt == 0) {
                        metricsRecorder.retrySucceededNotRetried();
                    } else {
                        metricsRecorder.retrySucceededRetried();
                    }
                    result.complete(value);
                } else {
                    metricsRecorder.retryFailed();
                    if (shouldAbortRetrying(exception)) {
                        metricsRecorder.retryFailed();
                        result.completeExceptionally(exception);
                    } else {
                        propagateCompletion(doRetry(target, attempt + 1, stopwatch, exception), result);
                    }
                }
            });

            return result;
        } catch (Throwable e) {
            if (shouldAbortRetrying(e)) {
                return failedStage(e);
            } else {
                return doRetry(target, attempt + 1, stopwatch, e);
            }
        }
    }
}
