package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.fallback.FallbackLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class Fallback<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final FallbackFunction<V> fallback;
    private final ExceptionDecision exceptionDecision;

    public Fallback(FaultToleranceStrategy<V> delegate, String description,
            FallbackFunction<V> fallback, ExceptionDecision exceptionDecision) {
        this.delegate = checkNotNull(delegate, "Fallback delegate must be set");
        this.description = checkNotNull(description, "Fallback description must be set");
        this.fallback = checkNotNull(fallback, "Fallback function must be set");
        this.exceptionDecision = checkNotNull(exceptionDecision, "Exception decision must be set");
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        FallbackFunction<V> fallback = ctx.get(FallbackFunction.class, this.fallback);
        ExceptionDecision exceptionDecision = ctx.get(ExceptionDecision.class, this.exceptionDecision);

        // required for `@ApplyGuard`
        if (fallback == FallbackFunction.IGNORE || exceptionDecision == ExceptionDecision.IGNORE) {
            return delegate.apply(ctx);
        }

        LOG.trace("Fallback started");
        try {
            ctx.fireEvent(FallbackEvents.Defined.INSTANCE);

            Completer<V> result = Completer.create();

            Future<V> originalResult;
            try {
                originalResult = delegate.apply(ctx);
            } catch (Exception e) {
                originalResult = Future.ofError(e);
            }

            originalResult.then((value, error) -> {
                if (error == null) {
                    result.complete(value);
                    return;
                }

                if (ctx.isSync()) {
                    if (error instanceof InterruptedException) {
                        result.completeWithError(error);
                        return;
                    } else if (Thread.interrupted()) {
                        result.completeWithError(new InterruptedException());
                        return;
                    }
                }

                if (exceptionDecision.isConsideredExpected(error)) {
                    result.completeWithError(error);
                    return;
                }

                try {
                    LOG.debugf("%s invocation failed, invoking fallback", description);
                    ctx.fireEvent(FallbackEvents.Applied.INSTANCE);
                    fallback.apply(new FailureContext(error, ctx)).thenComplete(result);
                } catch (Exception e) {
                    result.completeWithError(e);
                }
            });

            return result.future();
        } finally {
            LOG.trace("Fallback finished");
        }
    }
}
