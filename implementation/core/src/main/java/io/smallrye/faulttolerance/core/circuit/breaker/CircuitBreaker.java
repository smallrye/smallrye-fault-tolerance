package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.stopwatch.RunningStopwatch;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.timer.Timer;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class CircuitBreaker<V> implements FaultToleranceStrategy<V> {
    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_HALF_OPEN = 2;

    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final ExceptionDecision exceptionDecision;
    private final long delayInMillis;
    private final int rollingWindowSize;
    private final int failureThreshold;
    private final int successThreshold;
    private final Stopwatch stopwatch;
    private final Timer timer;

    private final AtomicReference<State> state;

    public CircuitBreaker(FaultToleranceStrategy<V> delegate, String description, ExceptionDecision exceptionDecision,
            long delayInMillis, int requestVolumeThreshold, double failureRatio, int successThreshold,
            Stopwatch stopwatch, Timer timer) {
        this.delegate = checkNotNull(delegate, "Circuit breaker delegate must be set");
        this.description = checkNotNull(description, "Circuit breaker description must be set");
        this.exceptionDecision = checkNotNull(exceptionDecision, "Exception decision must be set");
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Circuit breaker delay must be >= 0");
        this.successThreshold = check(successThreshold, successThreshold > 0, "Circuit breaker success threshold must be > 0");
        this.stopwatch = checkNotNull(stopwatch, "Stopwatch must be set");
        this.timer = checkNotNull(timer, "Timer must be set");
        this.failureThreshold = check((int) Math.ceil(failureRatio * requestVolumeThreshold),
                failureRatio >= 0.0 && failureRatio <= 1.0,
                "Circuit breaker rolling window failure ratio must be >= 0 && <= 1");
        this.rollingWindowSize = check(requestVolumeThreshold, requestVolumeThreshold > 0,
                "Circuit breaker rolling window size must be > 0");

        this.state = new AtomicReference<>(State.closed(rollingWindowSize, failureThreshold));
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        LOG.trace("CircuitBreaker started");
        try {
            // this is the only place where `state` can be dereferenced!
            // it must be passed through as a parameter to all the state methods,
            // so that they don't see the circuit breaker moving to a different state under them
            State currentState = state.get();
            switch (currentState.id) {
                case STATE_CLOSED:
                    return inClosed(ctx, currentState);
                case STATE_OPEN:
                    return inOpen(ctx, currentState);
                case STATE_HALF_OPEN:
                    return inHalfOpen(ctx, currentState);
                default:
                    throw new AssertionError("Invalid circuit breaker state: " + currentState.id);
            }
        } finally {
            LOG.trace("CircuitBreaker finished");
        }
    }

    private Future<V> inClosed(FaultToleranceContext<V> ctx, State state) {
        try {
            LOG.trace("Circuit breaker closed, invocation allowed");

            Completer<V> result = Completer.create();

            delegate.apply(ctx).then((value, error) -> {
                if (error == null) {
                    inClosedHandleResult(true, ctx, state);
                    result.complete(value);
                } else {
                    inClosedHandleResult(exceptionDecision.isConsideredExpected(error), ctx, state);
                    result.completeWithError(error);
                }
            });

            return result.future();
        } catch (Throwable e) {
            inClosedHandleResult(exceptionDecision.isConsideredExpected(e), ctx, state);
            return Future.ofError(e);
        }
    }

    private void inClosedHandleResult(boolean isSuccess, FaultToleranceContext<V> ctx, State state) {
        ctx.fireEvent(isSuccess ? CircuitBreakerEvents.Finished.SUCCESS : CircuitBreakerEvents.Finished.FAILURE);
        boolean failureThresholdReached = isSuccess
                ? state.rollingWindow.recordSuccess()
                : state.rollingWindow.recordFailure();
        if (failureThresholdReached) {
            LOG.trace("Failure threshold reached, circuit breaker moving to open");
            toOpen(ctx, state);
        }
    }

    private Future<V> inOpen(FaultToleranceContext<V> ctx, State state) {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            LOG.debugOrTrace(description + " invocation prevented by circuit breaker",
                    "Circuit breaker open, invocation prevented");
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            return Future.ofError(new CircuitBreakerOpenException(description + " circuit breaker is open"));
        } else {
            LOG.trace("Delay elapsed synchronously, circuit breaker moving to half-open");
            toHalfOpen(ctx, state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            // this is the only place where `state` can be dereferenced!
            // it must be passed through as a parameter to all the state methods,
            // so that they don't see the circuit breaker moving to a different state under them
            State currentState = this.state.get();
            switch (currentState.id) {
                case STATE_CLOSED:
                    return inClosed(ctx, currentState);
                case STATE_OPEN:
                    return inOpen(ctx, currentState);
                case STATE_HALF_OPEN:
                    return inHalfOpen(ctx, currentState);
                default:
                    throw new AssertionError("Invalid circuit breaker state: " + currentState.id);
            }
        }
    }

    private Future<V> inHalfOpen(FaultToleranceContext<V> ctx, State state) {
        if (state.probeAttempts.incrementAndGet() > successThreshold) {
            LOG.debugOrTrace(description + " invocation prevented by circuit breaker",
                    "Circuit breaker half-open, invocation prevented");
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            return Future.ofError(new CircuitBreakerOpenException(description + " circuit breaker is half-open"));
        }

        try {
            LOG.trace("Circuit breaker half-open, probe invocation allowed");

            Completer<V> result = Completer.create();

            delegate.apply(ctx).then((value, error) -> {
                if (error == null) {
                    inHalfOpenHandleResult(true, ctx, state);
                    result.complete(value);
                } else {
                    inHalfOpenHandleResult(exceptionDecision.isConsideredExpected(error), ctx, state);
                    result.completeWithError(error);
                }
            });

            return result.future();
        } catch (Throwable e) {
            inHalfOpenHandleResult(exceptionDecision.isConsideredExpected(e), ctx, state);
            return Future.ofError(e);
        }
    }

    private void inHalfOpenHandleResult(boolean isSuccess, FaultToleranceContext<V> ctx, State state) {
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

    void toClosed(FaultToleranceContext<V> ctx, State state) {
        State newState = State.closed(rollingWindowSize, failureThreshold);
        boolean moved = this.state.compareAndSet(state, newState);

        if (moved) {
            ctx.fireEvent(CircuitBreakerEvents.StateTransition.TO_CLOSED);
        }
    }

    void toOpen(FaultToleranceContext<V> ctx, State state) {
        State newState = State.open(stopwatch);
        boolean moved = this.state.compareAndSet(state, newState);

        if (moved) {
            ctx.fireEvent(CircuitBreakerEvents.StateTransition.TO_OPEN);

            // this is not necessary for correct functioning of the circuit breaker itself, because
            // all the necessary state transitions happen synchronously (during invocations)
            //
            // that, however, isn't enough for correct functioning of _external observers_ of
            // the circuit breaker state
            //
            // note that:
            // 1. the timer task created below and the circuit breaker invocations compete on
            //    changing the state, but that doesn't pose a problem, because it is an atomic CAS
            // 2. there's some overhead from having both synchronous and asynchronous state changes,
            //    but it's minuscule (see what `toHalfOpen` does)
            // 3. this asynchronous state transition fires the event to an _old_ `InvocationContext`,
            //    so if there's an event handler registered _after_ this circuit breaker invocation,
            //    it will _not_ be called (I don't think that's a problem, frankly)
            timer.schedule(delayInMillis, () -> {
                LOG.trace("Delay elapsed asynchronously, circuit breaker moving to half-open");
                toHalfOpen(ctx, newState);
            });
        }
    }

    void toHalfOpen(FaultToleranceContext<V> ctx, State state) {
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
        AtomicInteger probeAttempts; // only consulted in HALF_OPEN
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
            result.probeAttempts = new AtomicInteger(0);
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
