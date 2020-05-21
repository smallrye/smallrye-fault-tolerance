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
    public CompletionStageRetry(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            SetOfThrowables retryOn, SetOfThrowables abortOn, long maxRetries, long maxTotalDurationInMillis,
            Delay delayBetweenRetries, Stopwatch stopwatch) {
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, delayBetweenRetries, stopwatch);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        RunningStopwatch runningStopwatch = stopwatch.start();
        return doRetry(ctx, 0, runningStopwatch, null);
    }

    public CompletionStage<V> doRetry(InvocationContext<CompletionStage<V>> ctx, int attempt,
            RunningStopwatch stopwatch, Throwable latestFailure) {
        if (attempt == 0) {
            // do not sleep
        } else if (attempt <= maxRetries) {
            ctx.fireEvent(RetryEvents.Retried.INSTANCE);

            try {
                delayBetweenRetries.sleep();
            } catch (InterruptedException e) {
                ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                return failedStage(e);
            } catch (Exception e) {
                ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                if (Thread.interrupted()) {
                    return failedStage(new InterruptedException());
                }

                return failedStage(e);
            }
        } else {
            ctx.fireEvent(RetryEvents.Finished.MAX_RETRIES_REACHED);
            if (latestFailure != null) {
                return failedStage(latestFailure);
            } else {
                return failedStage(new FaultToleranceException(description + " reached max retries"));
            }
        }

        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            ctx.fireEvent(RetryEvents.Finished.MAX_DURATION_REACHED);
            if (latestFailure != null) {
                return failedStage(latestFailure);
            } else {
                return failedStage(new FaultToleranceException(description + " reached max retry duration"));
            }
        }

        try {
            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(ctx).whenComplete((value, exception) -> {
                if (exception == null) {
                    ctx.fireEvent(RetryEvents.Finished.VALUE_RETURNED);
                    result.complete(value);
                } else {
                    if (shouldAbortRetrying(exception)) {
                        ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                        result.completeExceptionally(exception);
                    } else {
                        propagateCompletion(doRetry(ctx, attempt + 1, stopwatch, exception), result);
                    }
                }
            });

            return result;
        } catch (Throwable e) {
            if (shouldAbortRetrying(e)) {
                ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                return failedStage(e);
            } else {
                return doRetry(ctx, attempt + 1, stopwatch, e);
            }
        }
    }
}
