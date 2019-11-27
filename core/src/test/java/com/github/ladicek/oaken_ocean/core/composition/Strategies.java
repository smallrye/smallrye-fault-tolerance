package com.github.ladicek.oaken_ocean.core.composition;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreaker;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreakerListener;
import com.github.ladicek.oaken_ocean.core.fallback.Fallback;
import com.github.ladicek.oaken_ocean.core.retry.Delay;
import com.github.ladicek.oaken_ocean.core.retry.Retry;
import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

import java.util.Collections;

/**
 * Factory methods for fault tolerance strategies that are easier to use than their constructors.
 * This is to be used for testing strategies composition, where one doesn't have to verify all possible
 * behaviors of the strategy (they are covered by unit tests of individual strategies).
 */
final class Strategies {
    static Fallback<String> fallback(FaultToleranceStrategy<String> delegate) {
        return new Fallback<>(delegate, "fallback", e -> "fallback after " + e.getClass().getSimpleName());
    }

    static <V> Retry<V> retry(FaultToleranceStrategy<V> delegate) {
        return new Retry<>(delegate, "retry",
                SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class)),
                SetOfThrowables.EMPTY, 10, 0, Delay.NONE, new TestStopwatch());
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate, CircuitBreakerListener listener) {
        return circuitBreaker(delegate, 0, listener);
    }

    static <V> CircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V> delegate, int delayInMillis, CircuitBreakerListener listener) {
        CircuitBreaker<V> result = new CircuitBreaker<>(delegate, "circuit breaker", SetOfThrowables.ALL,
                delayInMillis, 5, 0.2, 3, new TestStopwatch());
        result.addListener(listener);
        return result;
    }
}
