package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class CircuitBreaker<V> implements Callable<V> {
    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_HALF_OPEN = 2;

    private final Callable<V> delegate;
    private final String description;

    private final SetOfThrowables failOn;
    private final long delayInMillis;
    private final int rollingWindowSize;
    private final int failureThreshold;
    private final int successThreshold;
    private final Stopwatch stopwatch;

    // these state variables can only be mutated in the state transition methods (from*to*)
    private int state = STATE_CLOSED;
    private RollingWindow rollingWindow; // only consulted in CLOSED
    private RunningStopwatch runningStopwatch; // only consulted in OPEN
    private AtomicInteger consecutiveSuccesses; // only consulted in HALF_OPEN

    public CircuitBreaker(Callable<V> delegate, String description, SetOfThrowables failOn, long delayInMillis,
                          int requestVolumeThreshold, double failureRatio, int successThreshold, Stopwatch stopwatch) {
        this.delegate = checkNotNull(delegate, "Circuit breaker action must be set");
        this.description = checkNotNull(description, "Circuit breaker action description must be set");
        this.failOn = checkNotNull(failOn, "Set of fail-on throwables must be set");
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Circuit breaker delay must be >= 0");
        this.rollingWindowSize = check(requestVolumeThreshold, requestVolumeThreshold > 0, "Circuit breaker rolling window size must be > 0");
        this.failureThreshold = check((int) (failureRatio * requestVolumeThreshold), failureRatio >= 0.0 && failureRatio <= 1.0, "Circuit breaker rolling window failure ratio must be >= 0 && <= 1");
        this.successThreshold = check(successThreshold, successThreshold > 0, "Circuit breaker success threshold must be > 0");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");

        this.rollingWindow = RollingWindow.create(rollingWindowSize, failureThreshold);
    }

    @Override
    public V call() throws Exception {
        int state = this.state;
        switch (state) {
            case STATE_CLOSED:
                return inClosed();
            case STATE_OPEN:
                return inOpen();
            case STATE_HALF_OPEN:
                return inHalfOpen();
            default:
                throw new AssertionError("Invalid circuit breaker state: " + state);
        }
    }

    private V inClosed() throws Exception {
        try {
            V result = delegate.call();
            boolean failureThresholdReached = rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                fromClosedToOpen();
            }
            return result;
        } catch (Throwable e) {
            boolean failureThresholdReached = failOn.includes(e.getClass())
                    ? rollingWindow.recordFailure() : rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                fromClosedToOpen();
            }
            throw e;
        }
    }

    private V inOpen() throws Exception {
        if (runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            throw new CircuitBreakerOpenException(description + " circuit breaker is open");
        } else {
            fromOpenToHalfOpen();
            return inHalfOpen();
        }
    }

    private V inHalfOpen() throws Exception {
        try {
            V result = delegate.call();
            int successes = consecutiveSuccesses.incrementAndGet();
            if (successes >= successThreshold) {
                fromHalfOpenToClosed();
            }
            return result;
        } catch (Throwable e) {
            fromHalfOpenToOpen();
            throw e;
        }
    }

    // the state transitions must be happen atomically
    // currently, the methods are synchronized, but it should be possible to embed all the state variables
    // into an extra class, hold an AtomicReference to it and do a CAS

    private synchronized void fromClosedToOpen() {
        if (state == STATE_CLOSED) {
            state = STATE_OPEN;
            runningStopwatch = stopwatch.start();
        }
    }

    private synchronized void fromOpenToHalfOpen() {
        if (state == STATE_OPEN) {
            state = STATE_HALF_OPEN;
            consecutiveSuccesses = new AtomicInteger(0);
        }
    }

    private synchronized void fromHalfOpenToClosed() {
        if (state == STATE_HALF_OPEN) {
            state = STATE_CLOSED;
            rollingWindow = RollingWindow.create(rollingWindowSize, failureThreshold);
        }
    }

    private synchronized void fromHalfOpenToOpen() {
        if (state == STATE_HALF_OPEN) {
            state = STATE_OPEN;
            runningStopwatch = stopwatch.start();
        }
    }
}
