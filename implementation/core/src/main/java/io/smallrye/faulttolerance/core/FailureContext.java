package io.smallrye.faulttolerance.core;

import java.util.Objects;

public final class FailureContext {
    public final Throwable failure;
    public final InvocationContext<?> invocationContext;

    public FailureContext(Throwable failure, InvocationContext<?> invocationContext) {
        this.failure = Objects.requireNonNull(failure);
        this.invocationContext = Objects.requireNonNull(invocationContext);
    }
}
