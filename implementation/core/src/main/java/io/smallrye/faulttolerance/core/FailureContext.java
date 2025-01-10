package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

public final class FailureContext {
    public final Throwable failure;
    public final FaultToleranceContext<?> context;

    public FailureContext(Throwable failure, FaultToleranceContext<?> context) {
        this.failure = checkNotNull(failure, "failure must be set");
        this.context = checkNotNull(context, "context must be set");
    }
}
