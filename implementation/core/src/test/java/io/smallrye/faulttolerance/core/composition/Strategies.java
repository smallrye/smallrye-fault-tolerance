package io.smallrye.faulttolerance.core.composition;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.retry.AsyncDelay;
import io.smallrye.faulttolerance.core.retry.Retry;
import io.smallrye.faulttolerance.core.retry.SyncDelay;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.timer.TestTimer;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;
import io.smallrye.faulttolerance.core.util.ResultDecision;

/**
 * Factory methods for fault tolerance strategies that are easier to use than their constructors.
 * This is to be used for testing strategies composition, where one doesn't have to verify all possible
 * behaviors of the strategy (they are covered by unit tests of individual strategies).
 */
final class Strategies {
    static Fallback<String> fallback(FaultToleranceStrategy<String> delegate) {
        return new Fallback<>(delegate, "fallback",
                ctx -> Future.of("fallback after " + ctx.failure.getClass().getSimpleName()),
                ExceptionDecision.ALWAYS_FAILURE);
    }

    static <V> Retry<V> retry(FaultToleranceStrategy<V> delegate) {
        return new Retry<>(delegate, "retry", ResultDecision.ALWAYS_EXPECTED, ExceptionDecision.ALWAYS_FAILURE,
                10, 0, SyncDelay.NONE, AsyncDelay.NONE, new TestStopwatch(), null);
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate) {
        return circuitBreaker(delegate, 0);
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate, int delayInMillis) {
        return new CircuitBreaker<>(delegate, "circuit breaker", ExceptionDecision.ALWAYS_FAILURE,
                delayInMillis, 5, 0.2, 3, new TestStopwatch(), new TestTimer());
    }
}
