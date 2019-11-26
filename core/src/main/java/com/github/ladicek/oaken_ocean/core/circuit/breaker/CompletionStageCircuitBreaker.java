package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import com.github.ladicek.oaken_ocean.core.stopwatch.Stopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.checkNotNull;

public class CompletionStageCircuitBreaker extends CircuitBreaker {

    public CompletionStageCircuitBreaker(String description,
                                         SetOfThrowables failOn,
                                         long delayInMillis,
                                         int requestVolumeThreshold,
                                         double failureRatio,
                                         int successThreshold,
                                         Stopwatch stopwatch,
                                         MetricsRecorder metricsRecorder) {
        super(description, failOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold, stopwatch, metricsRecorder);
    }

    public <V> Callable<CompletionStage<V>> asyncCallable(final Callable<CompletionStage<V>> delegate) {
        checkNotNull(delegate, "Circuit breaker action must be set");
        return () -> {
            // this is the only place where `state` can be dereferenced!
            // it must be passed through as a parameter to all the state methods,
            // so that they don't see the circuit breaker moving to a different state under them
            return CompletionStageCircuitBreaker.this.performCall(delegate);
        };
    }

    private <V> CompletionStage<V> performCall(Callable<CompletionStage<V>> delegate) throws Exception {
        CircuitBreaker.State currentState = state.get();
        switch (currentState.id) {
            case STATE_CLOSED:
                return inClosed(delegate, currentState);
            case STATE_OPEN:
                return inOpen(delegate, currentState);
            case STATE_HALF_OPEN:
                return inHalfOpen(delegate, currentState);
            default:
                throw new AssertionError("Invalid circuit breaker state: " + currentState.id);
        }
    }


    private <V> CompletionStage<V> inClosed(Callable<CompletionStage<V>> delegate, CircuitBreaker.State state) throws Exception {
        try {
            CompletionStage<V> result = delegate.call();

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
            throw onFailure(state, e);
        }
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
              ? state.rollingWindow.recordFailure() : state.rollingWindow.recordSuccess();
        if (failureThresholdReached) {
            long now = System.nanoTime();

            openStart = now;
            metricsRecorder.circuitBreakerClosedTime(now - closedStart);
            metricsRecorder.circuitBreakerClosedToOpen();

            toOpen(state);
        }

        if (e instanceof CompletionException) {
            return (CompletionException) e;
        } else {
            return new CompletionException(e);
        }
    }

    private <V> CompletionStage<V> inOpen(Callable<CompletionStage<V>> delegate, CircuitBreaker.State state) throws Exception {
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
            return performCall(delegate);
        }
    }

    private <V> V inHalfOpen(Callable<V> delegate, CircuitBreaker.State state) throws Exception {
        try {
            V result = delegate.call();
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

    private void toClosed(CircuitBreaker.State state) {
        CircuitBreaker.State newState = CircuitBreaker.State.closed(rollingWindowSize, failureThreshold);
        this.state.compareAndSet(state, newState);
    }

    private void toOpen(CircuitBreaker.State state) {
        CircuitBreaker.State newState = CircuitBreaker.State.open(stopwatch);
        this.state.compareAndSet(state, newState);
    }

    private void toHalfOpen(CircuitBreaker.State state) {
        CircuitBreaker.State newState = CircuitBreaker.State.halfOpen();
        this.state.compareAndSet(state, newState);
    }

}
