package io.smallrye.faulttolerance.core.fallback;

import io.smallrye.faulttolerance.core.InvocationContext;

public final class FallbackContext<V> {
    public final Throwable failure;
    public final InvocationContext<V> invocationContext;

    public FallbackContext(Throwable failure, InvocationContext<V> invocationContext) {
        this.failure = failure;
        this.invocationContext = invocationContext;
    }
}
