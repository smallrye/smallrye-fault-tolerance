package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CircuitBreaker<V> implements FaultToleranceStrategy<V> {
    static final int STATE_CLOSED = 0;
    static final int STATE_OPEN = 1;
    static final int STATE_HALF_OPEN = 2;

    final FaultToleranceStrategy<V> delegate;
    final String description;

    final SetOfThrowables failOn;
    final SetOfThrowables skipOn;
    final long delayInMillis;
    final int rollingWindowSize;
    final int failureThreshold;
    final int successThreshold;
    final Stopwatch stopwatch;

    final List<CircuitBreakerListener> listeners = new CopyOnWriteArrayList<>();

    final AtomicReference<State> state;

    final MetricsRecorder metricsRecorder;

    final AtomicLong previousHalfOpenTime = new AtomicLong();
    volatile long halfOpenStart;
    final AtomicLong previousClosedTime = new AtomicLong();
    volatile long closedStart;
    final AtomicLong previousOpenTime = new AtomicLong();
    volatile long openStart;

    @SuppressWarnings("UnnecessaryThis")
    public CircuitBreaker(FaultToleranceStrategy<V> delegate, String description, SetOfThrowables failOn,
            SetOfThrowables skipOn, long delayInMillis, int requestVolumeThreshold, double failureRatio,
            int successThreshold, Stopwatch stopwatch, MetricsRecorder metricsRecorder) {
        this.delegate = checkNotNull(delegate, "Circuit breaker delegate must be set");
        this.description = checkNotNull(description, "Circuit breaker description must be set");
        this.failOn = checkNotNull(failOn, "Set of fail-on throwables must be set");
        this.skipOn = checkNotNull(skipOn, "Set of skip-on throwables must be set");
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Circuit breaker delay must be >= 0");
        this.successThreshold = check(successThreshold, successThreshold > 0, "Circuit breaker success threshold must be > 0");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
        this.failureThreshold = check((int) Math.ceil(failureRatio * requestVolumeThreshold),
                failureRatio >= 0.0 && failureRatio <= 1.0,
                "Circuit breaker rolling window failure ratio must be >= 0 && <= 1");
        this.rollingWindowSize = check(requestVolumeThreshold, requestVolumeThreshold > 0,
                "Circuit breaker rolling window size must be > 0");

        this.state = new AtomicReference<>(State.closed(rollingWindowSize, failureThreshold));

        this.metricsRecorder = metricsRecorder == null ? MetricsRecorder.NO_OP : metricsRecorder;
        this.closedStart = System.nanoTime();

        // todo: wrap this measurements in some object not to duplicate this logic
        this.metricsRecorder.circuitBreakerClosedTimeProvider(() -> getTime(STATE_CLOSED, closedStart, previousClosedTime));
        this.metricsRecorder
                .circuitBreakerHalfOpenTimeProvider(() -> getTime(STATE_HALF_OPEN, halfOpenStart, previousHalfOpenTime));

        this.metricsRecorder.circuitBreakerOpenTimeProvider(() -> getTime(STATE_OPEN, openStart, previousOpenTime));

    }

    private Long getTime(int measuredState, long measuredStateStart, AtomicLong prevMeasuredStateTime) {
        return state.get().id == measuredState
                ? prevMeasuredStateTime.get() + System.nanoTime() - measuredStateStart
                : prevMeasuredStateTime.get();
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        // this is the only place where `state` can be dereferenced!
        // it must be passed through as a parameter to all the state methods,
        // so that they don't see the circuit breaker moving to a different state under them
        State state = this.state.get();
        switch (state.id) {
            case STATE_CLOSED:
                return inClosed(ctx, state);
            case STATE_OPEN:
                return inOpen(ctx, state);
            case STATE_HALF_OPEN:
                return inHalfOpen(ctx, state);
            default:
                throw new AssertionError("Invalid circuit breaker state: " + state.id);
        }
    }

    boolean isConsideredSuccess(Throwable e) {
        return skipOn.includes(e.getClass()) || !failOn.includes(e.getClass());
    }

    private V inClosed(InvocationContext<V> context, State state) throws Exception {
        try {
            V result = delegate.apply(context);
            metricsRecorder.circuitBreakerSucceeded();
            boolean failureThresholdReached = state.rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                toOpen(state);
            }
            listeners.forEach(CircuitBreakerListener::succeeded);
            return result;
        } catch (Throwable e) {
            boolean isFailure = !isConsideredSuccess(e);
            if (isFailure) {
                listeners.forEach(CircuitBreakerListener::failed);
                metricsRecorder.circuitBreakerFailed();
            } else {
                listeners.forEach(CircuitBreakerListener::succeeded);
                metricsRecorder.circuitBreakerSucceeded();
            }
            boolean failureThresholdReached = isFailure
                    ? state.rollingWindow.recordFailure()
                    : state.rollingWindow.recordSuccess();
            if (failureThresholdReached) {
                long now = System.nanoTime();

                openStart = now;
                previousClosedTime.addAndGet(now - closedStart);
                metricsRecorder.circuitBreakerClosedToOpen();

                toOpen(state);
            }
            throw e;
        }
    }

    private V inOpen(InvocationContext<V> context, State state) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            metricsRecorder.circuitBreakerRejected();
            listeners.forEach(CircuitBreakerListener::rejected);
            throw new CircuitBreakerOpenException(description + " circuit breaker is open");
        } else {
            long now = System.nanoTime();

            halfOpenStart = now;
            previousOpenTime.addAndGet(now - openStart);

            toHalfOpen(state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return apply(context);
        }
    }

    private V inHalfOpen(InvocationContext<V> context, State state) throws Exception {
        try {
            V result = delegate.apply(context);
            metricsRecorder.circuitBreakerSucceeded();

            int successes = state.consecutiveSuccesses.incrementAndGet();
            if (successes >= successThreshold) {
                long now = System.nanoTime();
                closedStart = now;
                previousHalfOpenTime.addAndGet(now - halfOpenStart);

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

    void toClosed(State state) {
        State newState = State.closed(rollingWindowSize, failureThreshold);
        this.state.compareAndSet(state, newState);
    }

    void toOpen(State state) {
        State newState = State.open(stopwatch);
        this.state.compareAndSet(state, newState);
    }

    void toHalfOpen(State state) {
        State newState = State.halfOpen();
        this.state.compareAndSet(state, newState);
    }

    static final class State {
        final int id;
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

        void circuitBreakerOpenTimeProvider(Supplier<Long> supplier);

        void circuitBreakerHalfOpenTimeProvider(Supplier<Long> supplier);

        void circuitBreakerClosedTimeProvider(Supplier<Long> supplier);

        void circuitBreakerClosedToOpen();

        void circuitBreakerFailed();

        void circuitBreakerSucceeded();

        MetricsRecorder NO_OP = new MetricsRecorder() {
            @Override
            public void circuitBreakerRejected() {
            }

            @Override
            public void circuitBreakerOpenTimeProvider(Supplier<Long> supplier) {
            }

            @Override
            public void circuitBreakerHalfOpenTimeProvider(Supplier<Long> supplier) {
            }

            @Override
            public void circuitBreakerClosedTimeProvider(Supplier<Long> supplier) {
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
