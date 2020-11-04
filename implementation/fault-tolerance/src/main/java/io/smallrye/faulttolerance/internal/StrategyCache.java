package io.smallrye.faulttolerance.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.inject.Singleton;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@Singleton
public class StrategyCache {
    private final Map<InterceptionPoint, FaultToleranceStrategy<?>> strategies = new ConcurrentHashMap<>();
    private final Map<InterceptionPoint, MetricsRecorder> metricsRecorders = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <V> FaultToleranceStrategy<V> getStrategy(InterceptionPoint point,
            Supplier<FaultToleranceStrategy<V>> producer) {
        return (FaultToleranceStrategy<V>) strategies.computeIfAbsent(point, ignored -> producer.get());
    }

    public MetricsRecorder getMetrics(InterceptionPoint point, Supplier<MetricsRecorder> producer) {
        return metricsRecorders.computeIfAbsent(point, ignored -> producer.get());
    }
}
