package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.fallback.FallbackLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.ExceptionDecision;

public class CompletionStageFallback<V> extends Fallback<CompletionStage<V>> {
    public CompletionStageFallback(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            Function<FailureContext, CompletionStage<V>> fallback, ExceptionDecision exceptionDecision) {
        super(delegate, description, fallback, exceptionDecision);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageFallback started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageFallback finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        ctx.fireEvent(FallbackEvents.Defined.INSTANCE);

        CompletableFuture<V> result = new CompletableFuture<>();

        CompletionStage<V> originalResult;
        try {
            originalResult = delegate.apply(ctx);
        } catch (Exception e) {
            originalResult = failedFuture(e);
        }

        originalResult.whenComplete((value, exception) -> {
            if (exception == null) {
                result.complete(value);
                return;
            }

            if (shouldSkipFallback(exception)) {
                result.completeExceptionally(exception);
                return;
            }

            try {
                LOG.debugf("%s invocation failed, invoking fallback", description);
                ctx.fireEvent(FallbackEvents.Applied.INSTANCE);
                propagateCompletion(fallback.apply(new FailureContext(exception, ctx)), result);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }
}
