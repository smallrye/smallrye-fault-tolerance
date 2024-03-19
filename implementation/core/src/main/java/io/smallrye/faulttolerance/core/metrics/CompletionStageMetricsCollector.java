package io.smallrye.faulttolerance.core.metrics;

import static io.smallrye.faulttolerance.core.metrics.MetricsLogger.LOG;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageMetricsCollector<V> extends MetricsCollector<CompletionStage<V>> {
    public CompletionStageMetricsCollector(FaultToleranceStrategy<CompletionStage<V>> delegate, MetricsRecorder metrics,
            MeteredOperation operation) {
        super(delegate, metrics, operation);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageMetricsCollector started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageMetricsCollector finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        registerMetrics(ctx);

        CompletableFuture<V> result = new CompletableFuture<>();

        CompletionStage<V> originalResult;
        try {
            originalResult = delegate.apply(ctx);
        } catch (Exception e) {
            originalResult = failedFuture(e);
        }

        originalResult.whenComplete((value, exception) -> {
            if (exception == null) {
                ctx.fireEvent(GeneralMetricsEvents.ExecutionFinished.VALUE_RETURNED);
                result.complete(value);
            } else {
                ctx.fireEvent(GeneralMetricsEvents.ExecutionFinished.EXCEPTION_THROWN);
                result.completeExceptionally(exception);
            }
        });

        return result;
    }
}
