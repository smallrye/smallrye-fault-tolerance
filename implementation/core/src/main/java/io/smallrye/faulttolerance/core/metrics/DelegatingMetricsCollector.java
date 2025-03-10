package io.smallrye.faulttolerance.core.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public class DelegatingMetricsCollector<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final MetricsProvider provider;
    private final MeteredOperation originalOperation;

    private final ConcurrentMap<MeteredOperation, MetricsCollector<V>> cache = new ConcurrentHashMap<>();

    public DelegatingMetricsCollector(FaultToleranceStrategy<V> delegate,
            MetricsProvider provider, MeteredOperation originalOperation) {
        this.delegate = delegate;
        this.provider = provider;
        this.originalOperation = originalOperation;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        MeteredOperationName name = ctx.get(MeteredOperationName.class);
        MeteredOperation operation = name != null
                ? new DelegatingMeteredOperation(originalOperation, name.get())
                : originalOperation;
        FaultToleranceStrategy<V> delegate;
        if (operation.enabled()) {
            delegate = cache.computeIfAbsent(operation,
                    ignored -> new MetricsCollector<>(this.delegate, provider.create(operation), operation));
        } else {
            delegate = this.delegate;
        }
        return delegate.apply(ctx);
    }
}
