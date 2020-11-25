package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CircuitBreaker<V> implements FaultToleranceStrategy<V> {
    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_HALF_OPEN = 2;

    final FaultToleranceStrategy<V> delegate;
    final String description;

    final SetOfThrowables failOn;
    final SetOfThrowables skipOn;
    final long delayInMillis;
    final int rollingWindowSize;
    final int failureThreshold;
    final int successThreshold;
    final Stopwatch stopwatch;

    final AtomicReference<State> state;

    @SuppressWarnings("UnnecessaryThis")
    public CircuitBreaker(FaultToleranceStrategy<V> delegate, String description, SetOfThrowables failOn,
            SetOfThrowables skipOn, long delayInMillis, int requestVolumeThreshold, double failureRatio, int successThreshold,
            Stopwatch stopwatch) {
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
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("CircuitBreaker started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CircuitBreaker finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
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

    private V inClosed(InvocationContext<V> ctx, State state) throws Exception {
        try {
            LOG.trace("Circuit breaker closed, invocation allowed");
            V result = delegate.apply(ctx);
            inClosedHandleResult(true, ctx, state);
            return result;
        } catch (Throwable e) {
            inClosedHandleResult(isConsideredSuccess(e), ctx, state);
            throw e;
        }
    }

    final void inClosedHandleResult(boolean isSuccess, InvocationContext<V> ctx, State state) {
        ctx.fireEvent(isSuccess ? CircuitBreakerEvents.Finished.SUCCESS : CircuitBreakerEvents.Finished.FAILURE);
        boolean failureThresholdReached = isSuccess
                ? state.rollingWindow.recordSuccess()
                : state.rollingWindow.recordFailure();
        if (failureThresholdReached) {
            LOG.trace("Failure threshold reached, circuit breaker moving to open");
            toOpen(ctx, state);
        }
    }

    private V inOpen(InvocationContext<V> ctx, State state) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            LOG.trace("Circuit breaker open, invocation prevented");
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            throw new CircuitBreakerOpenException(description + " circuit breaker is open");
        } else {
            LOG.trace("Delay elapsed, circuit breaker moving to half-open");
            toHalfOpen(ctx, state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return apply(ctx);
        }
    }

    private V inHalfOpen(InvocationContext<V> ctx, State state) throws Exception {
        try {
            LOG.trace("Circuit breaker half-open, probe invocation allowed");
            V result = delegate.apply(ctx);
            inHalfOpenHandleResult(true, ctx, state);
            return result;
        } catch (Throwable e) {
            inHalfOpenHandleResult(isConsideredSuccess(e), ctx, state);
            throw e;
        }
    }

    final void inHalfOpenHandleResult(boolean isSuccess, InvocationContext<V> ctx, State state) {
        ctx.fireEvent(isSuccess ? CircuitBreakerEvents.Finished.SUCCESS : CircuitBreakerEvents.Finished.FAILURE);
        if (isSuccess) {
            int successes = state.consecutiveSuccesses.incrementAndGet();
            if (successes >= successThreshold) {
                LOG.trace("Success threshold reached, circuit breaker moving to closed");
                toClosed(ctx, state);
            }
        } else {
            LOG.trace("Failure while in half-open, circuit breaker moving to open");
            toOpen(ctx, state);
        }
    }

    void toClosed(InvocationContext<V> ctx, State state) {
        State newState = State.closed(rollingWindowSize, failureThreshold);
        boolean moved = this.state.compareAndSet(state, newState);

        if (moved) {
            ctx.fireEvent(CircuitBreakerEvents.StateTransition.TO_CLOSED);
        }
    }

    void toOpen(InvocationContext<V> ctx, State state) {
        State newState = State.open(stopwatch);
        boolean moved = this.state.compareAndSet(state, newState);

        if (moved) {
            ctx.fireEvent(CircuitBreakerEvents.StateTransition.TO_OPEN);
        }
    }

    void toHalfOpen(InvocationContext<V> ctx, State state) {
        State newState = State.halfOpen();
        boolean moved = this.state.compareAndSet(state, newState);

        if (moved) {
            ctx.fireEvent(CircuitBreakerEvents.StateTransition.TO_HALF_OPEN);
        }
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

    // maintenance

    public int currentState() {
        return this.state.get().id;
    }

    public void reset() {
        State newState = State.closed(rollingWindowSize, failureThreshold);
        this.state.set(newState);
    }
}
