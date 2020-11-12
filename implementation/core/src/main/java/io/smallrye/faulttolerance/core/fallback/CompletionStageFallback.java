package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.fallback.FallbackLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageFallback<V> extends Fallback<CompletionStage<V>> {
    public CompletionStageFallback(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            FallbackFunction<CompletionStage<V>> fallback, SetOfThrowables applyOn, SetOfThrowables skipOn) {
        super(delegate, description, fallback, applyOn, skipOn);
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
            originalResult = failedStage(e);
        }

        originalResult.whenComplete((value, exception) -> {
            if (exception == null) {
                result.complete(value);
                return;
            }

            if (exception instanceof InterruptedException || Thread.interrupted()) {
                result.completeExceptionally(new InterruptedException());
                return;
            }

            if (shouldSkipFallback(exception)) {
                result.completeExceptionally(exception);
                return;
            }

            try {
                LOG.trace("Invocation failed, invoking fallback");
                ctx.fireEvent(FallbackEvents.Applied.INSTANCE);
                FallbackContext<CompletionStage<V>> fallbackContext = new FallbackContext<>(exception, ctx);
                propagateCompletion(fallback.call(fallbackContext), result);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }
}
