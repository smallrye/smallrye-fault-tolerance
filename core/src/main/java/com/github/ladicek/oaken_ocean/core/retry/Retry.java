package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import java.util.concurrent.Callable;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class Retry<V> implements Callable<V> {
    private final Callable<V> delegate;
    private final String description;

    private final SetOfThrowables retryOn;
    private final SetOfThrowables abortOn;
    private final long maxRetries; // this is an `int` in MP FT, but `long` allows easier handling of "infinity"
    private final long maxTotalDurationInMillis;
    private final Delay delayBetweenRetries;
    private final Stopwatch stopwatch;

    public Retry(Callable<V> delegate, String description, SetOfThrowables retryOn, SetOfThrowables abortOn,
                 long maxRetries, long maxTotalDurationInMillis, Delay delayBetweenRetries, Stopwatch stopwatch) {
        this.delegate = checkNotNull(delegate, "Retry action must be set");
        this.description = checkNotNull(description, "Retry action description must be set");
        this.retryOn = checkNotNull(retryOn, "Set of retry-on throwables must be set");
        this.abortOn = checkNotNull(abortOn, "Set of abort-on throwables must be set");
        this.maxRetries = maxRetries < 0 ? Long.MAX_VALUE : maxRetries;
        this.maxTotalDurationInMillis = maxTotalDurationInMillis <= 0 ? Long.MAX_VALUE : maxTotalDurationInMillis;
        this.delayBetweenRetries = checkNotNull(delayBetweenRetries, "Delay must be set");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
    }

    @Override
    public V call() throws Exception {
        long counter = 0;
        RunningStopwatch runningStopwatch = stopwatch.start();
        while (counter <= maxRetries && runningStopwatch.elapsedTimeInMillis() < maxTotalDurationInMillis) {
            try {
                return delegate.call();
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

        throw new FaultToleranceException(description + " reached max retries or max retry duration");
    }
}
