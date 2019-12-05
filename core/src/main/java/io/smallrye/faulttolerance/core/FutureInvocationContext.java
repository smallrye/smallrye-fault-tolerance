package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An {@link InvocationContext} for asynchronous fault tolerance operations that return {@link Future}.
 * Aside of the underlying call it also carries {@link Cancellator} that is responsible for cancelling/interrupting
 * the asynchronous invocation
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public final class FutureInvocationContext<V> implements InvocationContext<Future<V>> {

    private final Cancellator cancellator;
    private final Callable<Future<V>> delegate;

    public FutureInvocationContext(Cancellator cancellator, Callable<Future<V>> delegate) {
        this.cancellator = cancellator;
        this.delegate = delegate;
    }

    @Override
    public Callable<Future<V>> getDelegate() {
        return delegate;
    }

    public Cancellator getCancellator() {
        return cancellator;
    }
}
