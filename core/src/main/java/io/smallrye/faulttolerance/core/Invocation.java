package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;

/**
 * A "sentinel" fault tolerance strategy that does no processing, it only invokes the guarded {@link Callable}.
 * This is supposed to be used as the last fault tolerance stragegy in a chain.
 * <p>
 * There's only one instance of this class, accessible using {@link #invocation()}.
 */
public final class Invocation<V, ContextType extends InvocationContext<V>> implements FaultToleranceStrategy<V, ContextType> {
    private static final Invocation<?, ?> INSTANCE = new Invocation<>();

    public static <V, ContextType extends InvocationContext<V>> Invocation<V, ContextType> invocation() {
        return (Invocation<V, ContextType>) INSTANCE;
    }

    private Invocation() {
    } // avoid instantiation

    @Override
    public V apply(ContextType target) throws Exception {
        return target.getDelegate().call();
    }
}
