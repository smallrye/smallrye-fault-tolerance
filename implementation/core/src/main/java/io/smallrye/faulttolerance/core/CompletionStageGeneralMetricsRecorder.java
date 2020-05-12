package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

// TODO better name?
public class CompletionStageGeneralMetricsRecorder<V> implements FaultToleranceStrategy<CompletionStage<V>> {
    private final FaultToleranceStrategy<CompletionStage<V>> delegate;
    private final GeneralMetrics metrics;

    public CompletionStageGeneralMetricsRecorder(FaultToleranceStrategy<CompletionStage<V>> delegate, GeneralMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        metrics.invoked();

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
            } else {
                metrics.failed();
                result.completeExceptionally(exception);
            }
        });

        return result;
    }
}
