package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

public final class FailureContext {
    public final Object result; // `null` when the invocation failed
    public final Throwable failure; // `null` when the invocation returned a result
    public final FaultToleranceContext<?> context;

    public FailureContext(Object result, Throwable failure, FaultToleranceContext<?> context) {
        this.result = result;
        this.failure = failure;
        this.context = checkNotNull(context, "context must be set");
    }
}
