package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageCircuitBreaker<V> extends CircuitBreaker<CompletionStage<V>> {

    public CompletionStageCircuitBreaker(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            SetOfThrowables failOn, SetOfThrowables skipOn, long delayInMillis, int requestVolumeThreshold, double failureRatio,
            int successThreshold, Stopwatch stopwatch) {
        super(delegate, description, failOn, skipOn, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold,
                stopwatch);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
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
    }

    private CompletionStage<V> inClosed(InvocationContext<CompletionStage<V>> ctx, State state) {
        try {
            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(ctx).whenComplete((value, error) -> {
                if (error != null) {
                    onFailure(ctx, state, error);
                    result.completeExceptionally(error);
                } else {
                    ctx.fireEvent(CircuitBreakerEvents.Finished.SUCCESS);
                    boolean failureThresholdReached = state.rollingWindow.recordSuccess();
                    if (failureThresholdReached) {
                        toOpen(ctx, state);
                    }
                    listeners.forEach(CircuitBreakerListener::succeeded);
                    result.complete(value);
                }
            });

            return result;
        } catch (Throwable e) {
            onFailure(ctx, state, e);
            return failedStage(e);
        }
    }

    private void onFailure(InvocationContext<CompletionStage<V>> ctx, State state, Throwable e) {
        boolean isFailure = !isConsideredSuccess(e);
        if (isFailure) {
            listeners.forEach(CircuitBreakerListener::failed);
            ctx.fireEvent(CircuitBreakerEvents.Finished.FAILURE);
        } else {
            listeners.forEach(CircuitBreakerListener::succeeded);
            ctx.fireEvent(CircuitBreakerEvents.Finished.SUCCESS);
        }
        boolean failureThresholdReached = isFailure
                ? state.rollingWindow.recordFailure()
                : state.rollingWindow.recordSuccess();
        if (failureThresholdReached) {
            toOpen(ctx, state);
        }
    }

    private CompletionStage<V> inOpen(InvocationContext<CompletionStage<V>> ctx, State state) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            listeners.forEach(CircuitBreakerListener::rejected);
            return failedStage(new CircuitBreakerOpenException(description + " circuit breaker is open"));
        } else {
            toHalfOpen(ctx, state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return apply(ctx);
        }
    }

    private CompletionStage<V> inHalfOpen(InvocationContext<CompletionStage<V>> ctx, State state) {
        try {
            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(ctx).whenComplete((value, error) -> {
                if (error != null) {
                    ctx.fireEvent(CircuitBreakerEvents.Finished.FAILURE);
                    listeners.forEach(CircuitBreakerListener::failed);
                    toOpen(ctx, state);
                    result.completeExceptionally(error);
                } else {
                    ctx.fireEvent(CircuitBreakerEvents.Finished.SUCCESS);

                    int successes = state.consecutiveSuccesses.incrementAndGet();
                    if (successes >= successThreshold) {
                        toClosed(ctx, state);
                    }
                    listeners.forEach(CircuitBreakerListener::succeeded);
                    result.complete(value);
                }
            });

            return result;
        } catch (Throwable e) {
            ctx.fireEvent(CircuitBreakerEvents.Finished.FAILURE);
            listeners.forEach(CircuitBreakerListener::failed);
            toOpen(ctx, state);
            return failedStage(e);
        }
    }
}
