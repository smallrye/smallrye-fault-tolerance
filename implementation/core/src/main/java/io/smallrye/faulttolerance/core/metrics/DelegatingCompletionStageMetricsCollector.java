package io.smallrye.faulttolerance.core.metrics;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class DelegatingCompletionStageMetricsCollector<V> implements FaultToleranceStrategy<CompletionStage<V>> {
    private final FaultToleranceStrategy<CompletionStage<V>> delegate;
    private final MetricsProvider provider;
    private final MeteredOperation originalOperation;

    private final ConcurrentMap<MeteredOperation, CompletionStageMetricsCollector<V>> cache = new ConcurrentHashMap<>();

    public DelegatingCompletionStageMetricsCollector(FaultToleranceStrategy<CompletionStage<V>> delegate,
            MetricsProvider provider, MeteredOperation originalOperation) {
        this.delegate = delegate;
        this.provider = provider;
        this.originalOperation = originalOperation;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) throws Exception {
        MeteredOperationName name = ctx.get(MeteredOperationName.class);
        MeteredOperation operation = name != null
                ? new DelegatingMeteredOperation(originalOperation, name.get())
                : originalOperation;
        CompletionStageMetricsCollector<V> delegate = cache.computeIfAbsent(operation,
                ignored -> new CompletionStageMetricsCollector<>(this.delegate, provider.create(operation), operation));
        return delegate.apply(ctx);
    }
}
