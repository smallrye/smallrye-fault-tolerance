package io.smallrye.faulttolerance.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.inject.Singleton;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.metrics.MetricsCollector;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class StrategyCache {
    private final Map<InterceptionPoint, FaultToleranceStrategy<?>> strategies = new ConcurrentHashMap<>();
    private final Map<InterceptionPoint, MetricsCollector> metricsCollectors = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <V> FaultToleranceStrategy<V> getStrategy(InterceptionPoint point,
            Function<InterceptionPoint, FaultToleranceStrategy<V>> producer) {
        return (FaultToleranceStrategy<V>) strategies.computeIfAbsent(point, producer);
    }

    public MetricsCollector getMetrics(InterceptionPoint point, Function<InterceptionPoint, MetricsCollector> producer) {
        return metricsCollectors.computeIfAbsent(point, producer);
    }
}
