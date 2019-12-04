package com.github.ladicek.oaken_ocean.core.composition;

import java.util.Collections;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.CircuitBreakerListener;
import com.github.ladicek.oaken_ocean.core.circuit.breaker.SyncCircuitBreaker;
import com.github.ladicek.oaken_ocean.core.fallback.SyncFallback;
import com.github.ladicek.oaken_ocean.core.retry.Delay;
import com.github.ladicek.oaken_ocean.core.retry.SyncRetry;
import com.github.ladicek.oaken_ocean.core.stopwatch.TestStopwatch;
import com.github.ladicek.oaken_ocean.core.util.SetOfThrowables;

/**
 * Factory methods for fault tolerance strategies that are easier to use than their constructors.
 * This is to be used for testing strategies composition, where one doesn't have to verify all possible
 * behaviors of the strategy (they are covered by unit tests of individual strategies).
 */
final class Strategies {
    static SyncFallback<String> fallback(FaultToleranceStrategy<String, SimpleInvocationContext<String>> delegate) {
        return new SyncFallback<>(delegate, "fallback", e -> "fallback after " + e.getClass().getSimpleName(), null);
    }

    static <V> SyncRetry<V> retry(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate) {
        return new SyncRetry<>(delegate, "retry",
                SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class)),
                SetOfThrowables.EMPTY, 10, 0, Delay.NONE, new TestStopwatch(), null);
    }

    static <V> SyncCircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate,
            CircuitBreakerListener listener) {
        return circuitBreaker(delegate, 0, listener);
    }

    static <V> SyncCircuitBreaker<V> circuitBreaker(FaultToleranceStrategy<V, SimpleInvocationContext<V>> delegate,
            int delayInMillis, CircuitBreakerListener listener) {
        SyncCircuitBreaker<V> result = new SyncCircuitBreaker<>(delegate, "circuit breaker", SetOfThrowables.ALL,
                delayInMillis, 5, 0.2, 3, new TestStopwatch(), null);
        result.addListener(listener);
        return result;
    }
}
