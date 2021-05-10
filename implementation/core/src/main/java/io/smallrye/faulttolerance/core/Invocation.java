package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.CoreLogger.LOG;

import java.util.concurrent.Callable;

/**
 * A "sentinel" fault tolerance strategy that does no processing, it only invokes the guarded {@link Callable}.
 * This is supposed to be used as the last fault tolerance stragegy in a chain.
 * <p>
 * There's only one instance of this class, accessible using {@link #invocation()}.
 */
public final class Invocation<V> implements FaultToleranceStrategy<V> {
    private static final Invocation<?> INSTANCE = new Invocation<>();

    @SuppressWarnings("unchecked")
    public static <V> Invocation<V> invocation() {
        return (Invocation<V>) INSTANCE;
    }

    private Invocation() {
        // avoid instantiation
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("Guarded method invocation started");
        try {
            return ctx.call();
        } finally {
            LOG.trace("Guarded method invocation finished");
        }
    }
}
