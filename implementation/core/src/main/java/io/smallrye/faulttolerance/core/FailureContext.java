package io.smallrye.faulttolerance.core;

import java.util.Objects;

public final class FailureContext {
    public final Throwable failure;
    public final FaultToleranceContext<?> context;

    public FailureContext(Throwable failure, FaultToleranceContext<?> context) {
        this.failure = Objects.requireNonNull(failure);
        this.context = Objects.requireNonNull(context);
    }
}
