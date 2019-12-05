package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;

/**
 * Basic {@link InvocationContext} that only provides the call to be executed.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SimpleInvocationContext<V> implements InvocationContext<V> {
    private final Callable<V> delegate;

    public SimpleInvocationContext(Callable<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Callable<V> getDelegate() {
        return delegate;
    }
}
