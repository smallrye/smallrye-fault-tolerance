package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.fallback.FallbackLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class Fallback<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    final String description;

    final FallbackFunction<V> fallback;
    private final ExceptionDecision exceptionDecision;

    public Fallback(FaultToleranceStrategy<V> delegate, String description, FallbackFunction<V> fallback,
            ExceptionDecision exceptionDecision) {
        this.delegate = checkNotNull(delegate, "Fallback delegate must be set");
        this.description = checkNotNull(description, "Fallback description must be set");
        this.fallback = checkNotNull(fallback, "Fallback function must be set");
        this.exceptionDecision = checkNotNull(exceptionDecision, "Exception decision must be set");
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("Fallback started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("Fallback finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
        ctx.fireEvent(FallbackEvents.Defined.INSTANCE);

        Throwable failure;
        try {
            return delegate.apply(ctx);
        } catch (Exception e) {
            failure = e;
        }

        if (failure instanceof InterruptedException || Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (shouldSkipFallback(failure)) {
            throw sneakyThrow(failure);
        }

        LOG.trace("Invocation failed, invoking fallback");
        ctx.fireEvent(FallbackEvents.Applied.INSTANCE);
        FallbackContext<V> fallbackContext = new FallbackContext<>(failure, ctx);
        return fallback.call(fallbackContext);
    }

    boolean shouldSkipFallback(Throwable e) {
        return exceptionDecision.isConsideredExpected(e);
    }
}
