package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

// mstodo read through to potentially simplify/reuse more from CircuitBreakerBase
// mstodo or make this class totally separate
public class CompletionStageCircuitBreaker<V>
        extends CircuitBreakerBase<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> {

    public CompletionStageCircuitBreaker(
            FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
            String description,
            SetOfThrowables failOn,
            long delayInMillis,
            int requestVolumeThreshold,
            double failureRatio,
            int successThreshold,
            Stopwatch stopwatch,
            MetricsRecorder metricsRecorder) {
        super(delegate, description, failOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold, stopwatch,
                metricsRecorder);
    }

    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> target) throws Exception {
        // this is the only place where `state` can be dereferenced!
        // it must be passed through as a parameter to all the state methods,
        // so that they don't see the circuit breaker moving to a different state under them
        SyncCircuitBreaker.State currentState = state.get();
        switch (currentState.id) {
            case STATE_CLOSED:
                return inClosed(target, currentState);
            case STATE_OPEN:
                return inOpen(target, currentState);
            case STATE_HALF_OPEN:
                return inHalfOpen(target, currentState);
            default:
                throw new AssertionError("Invalid circuit breaker state: " + currentState.id);
        }
    }

    private CompletionStage<V> inClosed(SimpleInvocationContext<CompletionStage<V>> target,
            SyncCircuitBreaker.State state) throws Exception {
        try {
            CompletionStage<V> result = delegate.apply(target);

            return result.handle((val, error) -> {
                if (error != null) {
                    throw onFailure(state, error);
                } else {
                    metricsRecorder.circuitBreakerSucceeded();
                    boolean failureThresholdReached = state.rollingWindow.recordSuccess();
                    if (failureThresholdReached) {
                        toOpen(state);
                    }
                    listeners.forEach(CircuitBreakerListener::succeeded);
                    return val;
                }
            });
        } catch (Throwable e) {
            CompletionException failure = onFailure(state, e);
            return failedCompletionStage(failure);
        }
    }

    private CompletionStage<V> failedCompletionStage(Throwable failure) {
        CompletableFuture<V> result = new CompletableFuture<>();
        result.completeExceptionally(failure);
        return result;
    }

    private CompletionException onFailure(State state, Throwable e) {
        boolean isFailure = failOn.includes(e.getClass());
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

        if (e instanceof CompletionException) {
            return (CompletionException) e;
        } else {
            return new CompletionException(e);
        }
    }

    private CompletionStage<V> inOpen(SimpleInvocationContext<CompletionStage<V>> target,
            SyncCircuitBreaker.State state) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            metricsRecorder.circuitBreakerRejected();
            listeners.forEach(CircuitBreakerListener::rejected);
            return failedCompletionStage(new CircuitBreakerOpenException(description + " circuit breaker is open"));
        } else {
            long now = System.nanoTime();

            halfOpenStart = now;
            previousOpenTime.addAndGet(now - openStart);

            toHalfOpen(state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return apply(target);
        }
    }

    private CompletionStage<V> inHalfOpen(SimpleInvocationContext<CompletionStage<V>> target, SyncCircuitBreaker.State state)
            throws Exception {
        try {
            CompletionStage<V> result = delegate.apply(target);
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
            return failedCompletionStage(e);
        }
    }

    private void toClosed(SyncCircuitBreaker.State state) {
        SyncCircuitBreaker.State newState = SyncCircuitBreaker.State.closed(rollingWindowSize, failureThreshold);
        this.state.compareAndSet(state, newState);
    }

    private void toOpen(SyncCircuitBreaker.State state) {
        SyncCircuitBreaker.State newState = SyncCircuitBreaker.State.open(stopwatch);
        this.state.compareAndSet(state, newState);
    }

    private void toHalfOpen(SyncCircuitBreaker.State state) {
        SyncCircuitBreaker.State newState = SyncCircuitBreaker.State.halfOpen();
        this.state.compareAndSet(state, newState);
    }

}
