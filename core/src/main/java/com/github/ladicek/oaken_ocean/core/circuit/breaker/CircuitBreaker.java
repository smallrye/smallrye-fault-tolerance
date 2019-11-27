package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.stopwatch.RunningStopwatch;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;
import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class CircuitBreaker<V> implements FaultToleranceStrategy<V> {
    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_HALF_OPEN = 2;

    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final SetOfThrowables failOn;
    private final long delayInMillis;
    private final int rollingWindowSize;
    private final int failureThreshold;
    private final int successThreshold;
    private final Stopwatch stopwatch;

    private final List<CircuitBreakerListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicReference<State> state;

    private final MetricsRecorder metricsRecorder;

    // TODO move to State?
    private volatile long halfOpenStart;
    private volatile long closedStart;
    private volatile long openStart;

    public CircuitBreaker(FaultToleranceStrategy<V> delegate, String description, SetOfThrowables failOn, long delayInMillis,
                          int requestVolumeThreshold, double failureRatio, int successThreshold, Stopwatch stopwatch,
                          MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Circuit breaker delegate must be set");
        this.description = checkNotNull(description, "Circuit breaker description must be set");
        this.failOn = checkNotNull(failOn, "Set of fail-on throwables must be set");
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Circuit breaker delay must be >= 0");
        this.rollingWindowSize = check(requestVolumeThreshold, requestVolumeThreshold > 0, "Circuit breaker rolling window size must be > 0");
        this.failureThreshold = check((int) (failureRatio * requestVolumeThreshold), failureRatio >= 0.0 && failureRatio <= 1.0, "Circuit breaker rolling window failure ratio must be >= 0 && <= 1");
        this.successThreshold = check(successThreshold, successThreshold > 0, "Circuit breaker success threshold must be > 0");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");

        this.state = new AtomicReference<>(State.closed(rollingWindowSize, failureThreshold));

        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
        this.closedStart = System.nanoTime();
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        // this is the only place where `state` can be dereferenced!
        // it must be passed through as a parameter to all the state methods,
        // so that they don't see the circuit breaker moving to a different state under them
        State state = this.state.get();
        switch (state.id) {
            case STATE_CLOSED:
                return inClosed(state, target);
            case STATE_OPEN:
                return inOpen(state, target);
            case STATE_HALF_OPEN:
                return inHalfOpen(state, target);
            default:
                throw new AssertionError("Invalid circuit breaker state: " + state.id);
        }
    }

    private V inClosed(State state, Callable<V> target) throws Exception {
        try {
            V result = delegate.apply(target);
            metricsRecorder.circuitBreakerSucceeded();
            boolean failureThresholdReached = state.rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                toOpen(state);
            }
            listeners.forEach(CircuitBreakerListener::succeeded);
            return result;
        } catch (Throwable e) {
            boolean isFailure = failOn.includes(e.getClass());
            if (isFailure) {
                listeners.forEach(CircuitBreakerListener::failed);
                metricsRecorder.circuitBreakerFailed();
            } else {
                listeners.forEach(CircuitBreakerListener::succeeded);
                metricsRecorder.circuitBreakerSucceeded();
            }
            boolean failureThresholdReached = isFailure
                    ? state.rollingWindow.recordFailure() : state.rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                long now = System.nanoTime();

                openStart = now;
                metricsRecorder.circuitBreakerClosedTime(now - closedStart);
                metricsRecorder.circuitBreakerClosedToOpen();

                toOpen(state);
            }
            throw e;
        }
    }

    private V inOpen(State state, Callable<V> target) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            metricsRecorder.circuitBreakerRejected();
            listeners.forEach(CircuitBreakerListener::rejected);
            throw new CircuitBreakerOpenException(description + " circuit breaker is open");
        } else {
            long now = System.nanoTime();

            halfOpenStart = now;
            metricsRecorder.circuitBreakerOpenTime(now - openStart);

            toHalfOpen(state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return apply(target);
        }
    }

    private V inHalfOpen(State state, Callable<V> target) throws Exception {
        try {
            V result = delegate.apply(target);
            metricsRecorder.circuitBreakerSucceeded();

            int successes = state.consecutiveSuccesses.incrementAndGet();
            if (successes >= successThreshold) {
                long now = System.nanoTime();
                closedStart = now;
                metricsRecorder.circuitBreakerHalfOpenTime(now - halfOpenStart);

                toClosed(state);
            }
            listeners.forEach(CircuitBreakerListener::succeeded);
            return result;
        } catch (Throwable e) {
            metricsRecorder.circuitBreakerFailed();
            listeners.forEach(CircuitBreakerListener::failed);
            toOpen(state);
            throw e;
        }
    }

    private void toClosed(State state) {
        State newState = State.closed(rollingWindowSize, failureThreshold);
        this.state.compareAndSet(state, newState);
    }

    private void toOpen(State state) {
        State newState = State.open(stopwatch);
        this.state.compareAndSet(state, newState);
    }

    private void toHalfOpen(State state) {
        State newState = State.halfOpen();
        this.state.compareAndSet(state, newState);
    }

    static final class State {
        int id;
        RollingWindow rollingWindow; // only consulted in CLOSED
        RunningStopwatch runningStopwatch; // only consulted in OPEN
        AtomicInteger consecutiveSuccesses; // only consulted in HALF_OPEN

        private State(int id) {
            this.id = id;
        }

        static State closed(int rollingWindowSize, int failureThreshold) {
            State result = new State(STATE_CLOSED);
            result.rollingWindow = RollingWindow.create(rollingWindowSize, failureThreshold);
            return result;
        }

        static State open(Stopwatch stopwatch) {
            State result = new State(STATE_OPEN);
            result.runningStopwatch = stopwatch.start();
            return result;
        }

        static State halfOpen() {
            State result = new State(STATE_HALF_OPEN);
            result.consecutiveSuccesses = new AtomicInteger(0);
            return result;
        }
    }

    public void addListener(CircuitBreakerListener listener) {
        listeners.add(listener);
    }

    public interface MetricsRecorder {
        void circuitBreakerRejected();
        void circuitBreakerOpenTime(long time);
        void circuitBreakerHalfOpenTime(long time);
        void circuitBreakerClosedTime(long time);
        void circuitBreakerClosedToOpen();
        void circuitBreakerFailed();
        void circuitBreakerSucceeded();

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void circuitBreakerRejected() {
            }

            @Override
            public void circuitBreakerOpenTime(long time) {
            }

            @Override
            public void circuitBreakerHalfOpenTime(long time) {
            }

            @Override
            public void circuitBreakerClosedTime(long time) {
            }

            @Override
            public void circuitBreakerClosedToOpen() {
            }

            @Override
            public void circuitBreakerFailed() {
            }

            @Override
            public void circuitBreakerSucceeded() {
            }
        };
    }
}
