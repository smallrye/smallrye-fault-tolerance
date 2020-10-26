package io.smallrye.faulttolerance.core.composition;

import java.util.Collections;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.fallback.Fallback;
import io.smallrye.faulttolerance.core.retry.Retry;
import io.smallrye.faulttolerance.core.retry.SyncDelay;
import io.smallrye.faulttolerance.core.stopwatch.TestStopwatch;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

/**
 * Factory methods for fault tolerance strategies that are easier to use than their constructors.
 * This is to be used for testing strategies composition, where one doesn't have to verify all possible
 * behaviors of the strategy (they are covered by unit tests of individual strategies).
 */
final class Strategies {
    static Fallback<String> fallback(FaultToleranceStrategy<String> delegate) {
        return new Fallback<>(delegate, "fallback", ctx -> "fallback after " + ctx.failure.getClass().getSimpleName(),
                SetOfThrowables.ALL, SetOfThrowables.EMPTY);
    }

    static <V> Retry<V> retry(FaultToleranceStrategy<V> delegate) {
        return new Retry<>(delegate, "retry",
                SetOfThrowables.create(Collections.singletonList(Exception.class)),
                SetOfThrowables.EMPTY, 10, 0, SyncDelay.NONE, new TestStopwatch());
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate) {
        return circuitBreaker(delegate, 0);
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate, int delayInMillis) {
        return new CircuitBreaker<>(delegate, "circuit breaker",
                SetOfThrowables.ALL, SetOfThrowables.EMPTY, delayInMillis, 5, 0.2, 3,
                new TestStopwatch());
    }
}
