package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;

/**
 * A fault tolerance strategy that guards invocations of arbitrary {@link Callable}s.
 * Fault tolerance strategies are expected to be nested; that is, implementations of this interface will typically delegate to
 * some other {@code FaultToleranceStrategy}.
 * The last strategy in the chain will invoke the guarded {@code Callable}; all other strategies are supposed to ignore it and
 * just pass it down the chain.
 * Usually, the last strategy will be {@link Invocation}.
 * <p>
 * The {@code Callable}s are wrapped in an {@link FaultToleranceContext}, which also provides support for out-of-band
 * communication between fault tolerance strategies in a single chain.
 * <p>
 * The strategies must be thread-safe, as they are expected to be used simultaneously from multiple threads.
 * This is important in case of strategies that maintain some state over time (such as circuit breaker).
 *
 * @param <V> the result type of method {@code apply}; also the result type of the guarded {@code Callable}
 */
@FunctionalInterface
public interface FaultToleranceStrategy<V> {
    /**
     * Apply the fault tolerance strategy around the target {@link Callable}.
     * The {@code Callable} is wrapped in an {@link FaultToleranceContext}.
     *
     * @param ctx the {@code InvocationContext} wrapping the {@code Callable} guarded by this fault tolerance strategy
     * @return result computed by the target {@code Callable}
     */
    Future<V> apply(FaultToleranceContext<V> ctx);
}
