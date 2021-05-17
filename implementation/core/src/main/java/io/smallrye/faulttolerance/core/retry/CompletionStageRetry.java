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
import io.smallrye.faulttolerance.core.util.DirectExecutor;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageRetry<V> extends Retry<CompletionStage<V>> {
    private final Supplier<AsyncDelay> delayBetweenRetries;

    public CompletionStageRetry(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            SetOfThrowables retryOn, SetOfThrowables abortOn, long maxRetries, long maxTotalDurationInMillis,
            Supplier<AsyncDelay> delayBetweenRetries, Stopwatch stopwatch) {
        // the SyncDelay.NONE is ignored here, we have our own AsyncDelay
        super(delegate, description, retryOn, abortOn, maxRetries, maxTotalDurationInMillis, SyncDelay.NONE, stopwatch);
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
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable latestFailure) {
        if (attempt == 0) {
            // do not sleep
            return afterDelay(ctx, attempt, delay, stopwatch, latestFailure);
        } else if (attempt <= maxRetries) {
            LOG.trace("Invocation failed, retrying");
            ctx.fireEvent(RetryEvents.Retried.INSTANCE);

            CompletableFuture<V> result = new CompletableFuture<>();

            // only to account for a potential Executor remembered by an earlier strategy;
            // that's why we use `DirectExecutor` otherwise
            Executor delayExecutor = ctx.get(Executor.class, DirectExecutor.INSTANCE);
            delay.after(() -> {
                delayExecutor.execute(() -> {
                    propagateCompletion(afterDelay(ctx, attempt, delay, stopwatch, latestFailure), result);
                });
            });

            return result;
        } else {
            ctx.fireEvent(RetryEvents.Finished.MAX_RETRIES_REACHED);
            if (latestFailure != null) {
                return failedStage(latestFailure);
            } else {
                return failedStage(new FaultToleranceException(description + " reached max retries"));
            }
        }
    }

    private CompletionStage<V> afterDelay(InvocationContext<CompletionStage<V>> ctx, int attempt,
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable latestFailure) {
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
