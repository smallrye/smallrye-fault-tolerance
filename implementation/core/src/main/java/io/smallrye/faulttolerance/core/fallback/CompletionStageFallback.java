package io.smallrye.faulttolerance.core.fallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.SetOfThrowables;

public class CompletionStageFallback<V> extends Fallback<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageFallback(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            FallbackFunction<CompletionStage<V>> fallback, SetOfThrowables applyOn, SetOfThrowables skipOn,
            Executor executor, MetricsRecorder metricsRecorder) {
        super(delegate, description, fallback, applyOn, skipOn, metricsRecorder);
        this.executor = executor;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            CompletionStage<V> originalResult;
            try {
                originalResult = delegate.apply(ctx);
            } catch (Exception e) {
                CompletableFuture<V> failure = new CompletableFuture<>();
                failure.completeExceptionally(e);
                originalResult = failure;
            }

            originalResult.whenComplete((value, exception) -> {
                if (value != null) {
                    result.complete(value);
                    return;
                }

                if (exception instanceof InterruptedException || Thread.interrupted()) {
                    result.completeExceptionally(new InterruptedException());
                    return;
                }

                if (shouldSkipFallback(exception)) {
                    result.completeExceptionally(exception);
                }

                try {
                    metricsRecorder.fallbackCalled();
                    fallback.call(exception).whenComplete((fallbackValue, fallbackException) -> {
                        if (fallbackValue != null) {
                            result.complete(fallbackValue);
                        } else {
                            result.completeExceptionally(fallbackException);
                        }
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        });

        return result;
    }
}
