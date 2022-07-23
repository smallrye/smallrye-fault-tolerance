package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.retry.RetryLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class CompletionStageRetry<V> extends Retry<CompletionStage<V>> {
    private final Supplier<AsyncDelay> delayBetweenRetries;

    public CompletionStageRetry(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            ExceptionDecision exceptionDecision, long maxRetries, long maxTotalDurationInMillis,
            Supplier<AsyncDelay> delayBetweenRetries, Stopwatch stopwatch) {
        // the SyncDelay.NONE is ignored here, we have our own AsyncDelay
        super(delegate, description, exceptionDecision, maxRetries, maxTotalDurationInMillis, SyncDelay.NONE, stopwatch);
        this.delayBetweenRetries = checkNotNull(delayBetweenRetries, "Delay must be set");
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageRetry started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageRetry finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        AsyncDelay delay = delayBetweenRetries.get();
        RunningStopwatch runningStopwatch = stopwatch.start();
        return doRetry(ctx, 0, delay, runningStopwatch, null);
    }

    private CompletionStage<V> doRetry(InvocationContext<CompletionStage<V>> ctx, int attempt,
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable lastFailure) {
        if (attempt == 0) {
            // do not sleep
            return afterDelay(ctx, attempt, delay, stopwatch, lastFailure);
        } else if (attempt <= maxRetries) {
            LOG.debug(description + " invocation failed, retrying (" + attempt + "/" + maxRetries + ")");
            ctx.fireEvent(RetryEvents.Retried.INSTANCE);

            CompletableFuture<V> result = new CompletableFuture<>();

            delay.after(lastFailure, () -> {
                propagateCompletion(afterDelay(ctx, attempt, delay, stopwatch, lastFailure), result);
            }, ctx.get(Executor.class));

            return result;
        } else {
            ctx.fireEvent(RetryEvents.Finished.MAX_RETRIES_REACHED);
            if (lastFailure != null) {
                return failedStage(lastFailure);
            } else {
                return failedStage(new FaultToleranceException(description + " reached max retries"));
            }
        }
    }

    private CompletionStage<V> afterDelay(InvocationContext<CompletionStage<V>> ctx, int attempt,
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable lastFailure) {
        if (stopwatch.elapsedTimeInMillis() > maxTotalDurationInMillis) {
            ctx.fireEvent(RetryEvents.Finished.MAX_DURATION_REACHED);
            if (lastFailure != null) {
                return failedStage(lastFailure);
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
                        propagateCompletion(doRetry(ctx, attempt + 1, delay, stopwatch, exception), result);
                    }
                }
            });

            return result;
        } catch (Throwable e) {
            if (shouldAbortRetrying(e)) {
                ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                return failedStage(e);
            } else {
                return doRetry(ctx, attempt + 1, delay, stopwatch, e);
            }
        }
    }
}
