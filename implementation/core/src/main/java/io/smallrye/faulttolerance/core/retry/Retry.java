package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.retry.RetryLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class Retry<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final SetOfThrowables retryOn;
    final SetOfThrowables abortOn;
    final long maxRetries; // this is an `int` in MP FT, but `long` allows easier handling of "infinity"
    final long maxTotalDurationInMillis;
    private final Supplier<SyncDelay> delayBetweenRetries;
    final Stopwatch stopwatch;

    public Retry(FaultToleranceStrategy<V> delegate, String description, SetOfThrowables retryOn, SetOfThrowables abortOn,
            long maxRetries, long maxTotalDurationInMillis, Supplier<SyncDelay> delayBetweenRetries, Stopwatch stopwatch) {
        this.delegate = checkNotNull(delegate, "Retry delegate must be set");
        this.description = checkNotNull(description, "Retry description must be set");
        this.retryOn = checkNotNull(retryOn, "Set of retry-on throwables must be set");
        this.abortOn = checkNotNull(abortOn, "Set of abort-on throwables must be set");
        this.maxRetries = maxRetries < 0 ? Long.MAX_VALUE : maxRetries;
        this.maxTotalDurationInMillis = maxTotalDurationInMillis <= 0 ? Long.MAX_VALUE : maxTotalDurationInMillis;
        this.delayBetweenRetries = checkNotNull(delayBetweenRetries, "Delay must be set");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("Retry started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("Retry finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        long counter = 0;
        SyncDelay delay = delayBetweenRetries.get();
        RunningStopwatch runningStopwatch = stopwatch.start();
        Throwable lastFailure = null;
        while (counter <= maxRetries && runningStopwatch.elapsedTimeInMillis() < maxTotalDurationInMillis) {
            if (counter > 0) {
                LOG.trace("Invocation failed, retrying");
                ctx.fireEvent(RetryEvents.Retried.INSTANCE);

                try {
                    delay.sleep(lastFailure);
                } catch (InterruptedException e) {
                    ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                    throw e;
                } catch (Exception e) {
                    ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    throw e;
                }

                // Previously, we called `delayBetweenRetries.sleep()` _after_ `delegate.apply()`
                // in a single iteration of the loop and had no additional check (it would happen
                // immediately as part of the loop condition). This has one issue: we would sleep
                // after the last retry iteration (per `maxRetries`), which is a waste of resources.
                // Currently, we call `delayBetweenRetries.sleep()` _before_ `delegate.apply()`
                // in a single iteration of the loop. This means we might call `delegate.apply()`
                // after sleeping, even though the time budget (per `maxTotalDurationInMillis`)
                // is possibly already empty. This would probably be OK per the spec (the TCK
                // doesn't care), but it would change our own existing behavior (as codified by
                // `RetryTest`). Hence this second explicit check.
                if (runningStopwatch.elapsedTimeInMillis() >= maxTotalDurationInMillis) {
                    break;
                }
            }

            try {
                V result = delegate.apply(ctx);
                ctx.fireEvent(RetryEvents.Finished.VALUE_RETURNED);
                return result;
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                if (Thread.interrupted()) {
                    ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                    throw new InterruptedException();
                }

                if (shouldAbortRetrying(e)) {
                    ctx.fireEvent(RetryEvents.Finished.EXCEPTION_NOT_RETRYABLE);
                    throw e;
                }

                lastFailure = e;
            }

            counter++;
        }

        if (counter > maxRetries) {
            ctx.fireEvent(RetryEvents.Finished.MAX_RETRIES_REACHED);
        } else {
            ctx.fireEvent(RetryEvents.Finished.MAX_DURATION_REACHED);
        }

        if (lastFailure != null) {
            throw sneakyThrow(lastFailure);
        } else {
            // this branch should never be taken
            throw new FaultToleranceException(description + " reached max retries or max retry duration");
        }
    }

    boolean shouldAbortRetrying(Throwable e) {
        // specifying `abortOn` is only useful when it's more specific than `retryOn`;
        // otherwise, if the exception isn't present in `retryOn`, it's always an abort
        return abortOn.includes(e.getClass()) || !retryOn.includes(e.getClass());
    }
}
