package io.smallrye.faulttolerance.core.circuit.breaker;

import static io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.stopwatch.Stopwatch;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class CompletionStageCircuitBreaker<V> extends CircuitBreaker<CompletionStage<V>> {

    public CompletionStageCircuitBreaker(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            ExceptionDecision exceptionDecision, long delayInMillis, int requestVolumeThreshold, double failureRatio,
            int successThreshold, Stopwatch stopwatch) {
        super(delegate, description, exceptionDecision, delayInMillis, requestVolumeThreshold, failureRatio, successThreshold,
                stopwatch);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
        LOG.trace("CompletionStageCircuitBreaker started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageCircuitBreaker finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
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
            LOG.trace("Circuit breaker closed, invocation allowed");

            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(ctx).whenComplete((value, error) -> {
                if (error != null) {
                    inClosedHandleResult(isConsideredSuccess(error), ctx, state);
                    result.completeExceptionally(error);
                } else {
                    inClosedHandleResult(true, ctx, state);
                    result.complete(value);
                }
            });

            return result;
        } catch (Throwable e) {
            inClosedHandleResult(isConsideredSuccess(e), ctx, state);
            return failedStage(e);
        }
    }

    private CompletionStage<V> inOpen(InvocationContext<CompletionStage<V>> ctx, State state) throws Exception {
        if (state.runningStopwatch.elapsedTimeInMillis() < delayInMillis) {
            LOG.trace("Circuit breaker open, invocation prevented");
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            return failedStage(new CircuitBreakerOpenException(description + " circuit breaker is open"));
        } else {
            LOG.trace("Delay elapsed, circuit breaker moving to half-open");
            toHalfOpen(ctx, state);
            // start over to re-read current state; no hard guarantee that it's HALF_OPEN at this point
            return doApply(ctx);
        }
    }

    private CompletionStage<V> inHalfOpen(InvocationContext<CompletionStage<V>> ctx, State state) {
        if (state.probeAttempts.incrementAndGet() > successThreshold) {
            LOG.trace("Circuit breaker half-open, invocation prevented");
            ctx.fireEvent(CircuitBreakerEvents.Finished.PREVENTED);
            return failedStage(new CircuitBreakerOpenException(description + " circuit breaker is half-open"));
        }

        try {
            LOG.trace("Circuit breaker half-open, probe invocation allowed");

            CompletableFuture<V> result = new CompletableFuture<>();

            delegate.apply(ctx).whenComplete((value, error) -> {
                if (error != null) {
                    inHalfOpenHandleResult(isConsideredSuccess(error), ctx, state);
                    result.completeExceptionally(error);
                } else {
                    inHalfOpenHandleResult(true, ctx, state);
                    result.complete(value);
                }
            });

            return result;
        } catch (Throwable e) {
            inHalfOpenHandleResult(isConsideredSuccess(e), ctx, state);
            return failedStage(e);
        }
    }
}
