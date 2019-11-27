package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import java.util.concurrent.Callable;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class Retry<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final SetOfThrowables retryOn;
    private final SetOfThrowables abortOn;
    private final long maxRetries; // this is an `int` in MP FT, but `long` allows easier handling of "infinity"
    private final long maxTotalDurationInMillis;
    private final Delay delayBetweenRetries;
    private final Stopwatch stopwatch;

    private volatile Thread executionThread;
    // mstodo reattempt should not be triggered before the previous one. So we should be good
    // mstodo move to the call class if we were to separate from the context class

    public Retry(FaultToleranceStrategy<V> delegate, String description, SetOfThrowables retryOn, SetOfThrowables abortOn,
                 long maxRetries, long maxTotalDurationInMillis, Delay delayBetweenRetries, Stopwatch stopwatch) {
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
    public V apply(Callable<V> target) throws Exception {
        executionThread = Thread.currentThread();
        long counter = 0;
        RunningStopwatch runningStopwatch = stopwatch.start();
        Throwable lastFailure = null;
        while (counter <= maxRetries && runningStopwatch.elapsedTimeInMillis() < maxTotalDurationInMillis) {
            try {
                return delegate.apply(target);
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // specifying `abortOn` is only useful when it's more specific than `retryOn`;
                // otherwise, if the exception isn't present in `retryOn`, it's always an abort
                if (abortOn.includes(e.getClass()) || !retryOn.includes(e.getClass())) {
                    throw e;
                }

                lastFailure = e;
            }

            try {
                delayBetweenRetries.sleep();
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                throw e;
            }

            counter++;
        }

        if (lastFailure != null) {
            // TODO: TCK expects that but it seems un(der)specified in the spec
            if (lastFailure instanceof Exception) {
                throw (Exception) lastFailure; // mstodo is it okay?
            } else {
                throw new FaultToleranceException(lastFailure.getMessage(), lastFailure);
            }
        } else {
            throw new FaultToleranceException(description + " reached max retries or max retry duration");
        }
    }

    public void cancel() {
        executionThread.interrupt(); // mstodo handle if needed
    }
}
