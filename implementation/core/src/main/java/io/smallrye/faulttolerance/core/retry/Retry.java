package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.retry.RetryLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;

public class Retry<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final ResultDecision resultDecision;
    private final ExceptionDecision exceptionDecision;
    private final long maxRetries; // this is an `int` in MP FT, but `long` allows easier handling of "infinity"
    private final long maxTotalDurationInMillis;
    private final Supplier<SyncDelay> syncDelayBetweenRetries;
    private final Supplier<AsyncDelay> asyncDelayBetweenRetries;
    private final Stopwatch stopwatch;
    private final Consumer<FailureContext> beforeRetry;

    public Retry(FaultToleranceStrategy<V> delegate, String description, ResultDecision resultDecision,
            ExceptionDecision exceptionDecision, long maxRetries, long maxTotalDurationInMillis,
            Supplier<SyncDelay> syncDelayBetweenRetries, Supplier<AsyncDelay> asyncDelayBetweenRetries,
            Stopwatch stopwatch, Consumer<FailureContext> beforeRetry) {
        this.delegate = checkNotNull(delegate, "Retry delegate must be set");
        this.description = checkNotNull(description, "Retry description must be set");
        this.resultDecision = checkNotNull(resultDecision, "Result decision must be set");
        this.exceptionDecision = checkNotNull(exceptionDecision, "Exception decision must be set");
        this.maxRetries = maxRetries < 0 ? Long.MAX_VALUE : maxRetries;
        this.maxTotalDurationInMillis = maxTotalDurationInMillis <= 0 ? Long.MAX_VALUE : maxTotalDurationInMillis;
        this.syncDelayBetweenRetries = checkNotNull(syncDelayBetweenRetries, "Synchronous delay must be set");
        this.asyncDelayBetweenRetries = checkNotNull(asyncDelayBetweenRetries, "Asynchronous delay must be set");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
        this.beforeRetry = beforeRetry;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        LOG.trace("Retry started");
        try {
            AsyncDelay delay = ctx.isAsync()
                    ? asyncDelayBetweenRetries.get()
                    : new SyncDelayAsAsync(syncDelayBetweenRetries.get());
            RunningStopwatch runningStopwatch = stopwatch.start();
            return doRetry(ctx, 0, delay, runningStopwatch, null);
        } finally {
            LOG.trace("Retry finished");
        }
    }

    private Future<V> doRetry(FaultToleranceContext<V> ctx, int attempt,
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable lastFailure) {
        if (attempt == 0) {
            // do not sleep
            return afterDelay(ctx, attempt, delay, stopwatch, lastFailure);
        } else if (attempt <= maxRetries) {
            if (stopwatch.elapsedTimeInMillis() >= maxTotalDurationInMillis) {
                ctx.fireEvent(RetryEvents.Finished.MAX_DURATION_REACHED);
                if (lastFailure != null) {
                    return Future.ofError(lastFailure);
                } else {
                    return Future.ofError(new FaultToleranceException(description + " reached max retry duration"));
                }
            }

            LOG.debugf("%s invocation failed, retrying (%d/%d)", description, attempt, maxRetries);
            ctx.fireEvent(RetryEvents.Retried.INSTANCE);

            Completer<V> result = Completer.create();

            try {
                delay.after(lastFailure, () -> {
                    afterDelay(ctx, attempt, delay, stopwatch, lastFailure).thenComplete(result);
                }, ctx.get(Executor.class));
            } catch (Exception e) {
                if (ctx.isSync() && Thread.interrupted()) {
                    result.completeWithError(new InterruptedException());
                } else {
                    result.completeWithError(e);
                }
            }

            return result.future();
        } else {
            ctx.fireEvent(RetryEvents.Finished.MAX_RETRIES_REACHED);
            if (lastFailure != null) {
                return Future.ofError(lastFailure);
            } else {
                return Future.ofError(new FaultToleranceException(description + " reached max retries"));
            }
        }
    }

    private Future<V> afterDelay(FaultToleranceContext<V> ctx, int attempt,
            AsyncDelay delay, RunningStopwatch stopwatch, Throwable lastFailure) {
        if (stopwatch.elapsedTimeInMillis() >= maxTotalDurationInMillis) {
            ctx.fireEvent(RetryEvents.Finished.MAX_DURATION_REACHED);
            if (lastFailure != null) {
                return Future.ofError(lastFailure);
            } else {
                return Future.ofError(new FaultToleranceException(description + " reached max retry duration"));
            }
        }

        if (beforeRetry != null && attempt > 0) {
            try {
                beforeRetry.accept(new FailureContext(lastFailure, ctx));
            } catch (Exception e) {
                LOG.warn("Before retry action has thrown an exception", e);
            }
        }

        Completer<V> result = Completer.create();
        try {
            delegate.apply(ctx).then((value, error) -> {
                if (ctx.isSync()) {
                    if (error instanceof InterruptedException) {
                        ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                        result.completeWithError(error);
                        return;
                    } else if (Thread.interrupted()) {
                        ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                        result.completeWithError(new InterruptedException());
                        return;
                    }
                }

                if (error == null) {
                    if (resultDecision.isConsideredExpected(value)) {
                        ctx.fireEvent(RetryEvents.Finished.VALUE_RETURNED);
                        result.complete(value);
                    } else {
                        doRetry(ctx, attempt + 1, delay, stopwatch, error).thenComplete(result);
                    }
                } else {
                    if (exceptionDecision.isConsideredExpected(error)) {
                        ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                        result.completeWithError(error);
                    } else {
                        doRetry(ctx, attempt + 1, delay, stopwatch, error).thenComplete(result);
                    }
                }
            });
        } catch (Throwable e) {
            if (exceptionDecision.isConsideredExpected(e)) {
                ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                result.completeWithError(e);
            } else {
                doRetry(ctx, attempt + 1, delay, stopwatch, e).thenComplete(result);
            }
        }
        return result.future();
    }
}
