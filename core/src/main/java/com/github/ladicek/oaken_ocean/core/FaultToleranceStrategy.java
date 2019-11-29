package com.github.ladicek.oaken_ocean.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A fault tolerance strategy that guards invocations of arbitrary {@link Callable}s.
 * Fault tolerance strategies are expected to be nested; that is, implementations of this interface will typically delegate to some other {@code FaultToleranceStrategy}.
 * The last strategy in the chain will invoke the guarded {@code Callable}; all other strategies are supposed to ignore it and just pass it down the chain.
 * Usually, the last strategy will be {@link Invocation}.
 * <p>
 * The strategies must be thread-safe, as they are expected to be used simultaneously from multiple threads.
 * This is important in case of strategies that maintain some state over time (such as circuit breaker).
 * @param <V> the result type of method {@code apply}; also the result type of the guarded {@code Callable}
 */
@FunctionalInterface
public interface FaultToleranceStrategy<V> {
    /**
     * Apply the fault tolerance strategy around the target {@link Callable}.
     *
     * @param target the {@code Callable} guarded by this fault tolerance strategy
     * @return result computed by the target {@code Callable}
     * @throws Exception if result couldn't be computed
     */
    V apply(Callable<V> target) throws Exception;


    default V asyncFutureApply(Callable<V> target, Cancelator cancelator) throws Exception {
        return apply(target);
    }
}
