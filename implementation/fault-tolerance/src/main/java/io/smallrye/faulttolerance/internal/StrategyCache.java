package io.smallrye.faulttolerance.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;

@Singleton
public class StrategyCache {
    private final Map<InterceptionPoint, FaultToleranceStrategy<?>> strategies = new ConcurrentHashMap<>();
    private final Map<InterceptionPoint, FallbackMethodCandidates> fallbackMethods = new ConcurrentHashMap<>();
    private final Map<InterceptionPoint, BeforeRetryMethod> beforeRetryMethods = new ConcurrentHashMap<>();

    private final SpecCompatibility specCompatibility;

    @Inject
    public StrategyCache(SpecCompatibility specCompatibility) {
        this.specCompatibility = specCompatibility;
    }

    @SuppressWarnings("unchecked")
    public <V> FaultToleranceStrategy<V> getStrategy(InterceptionPoint point,
            Supplier<FaultToleranceStrategy<V>> producer) {
        return (FaultToleranceStrategy<V>) strategies.computeIfAbsent(point, ignored -> producer.get());
    }

    public FallbackMethodCandidates getFallbackMethodCandidates(InterceptionPoint point, FaultToleranceOperation operation) {
        return fallbackMethods.computeIfAbsent(point, ignored -> FallbackMethodCandidates.create(
                operation, specCompatibility.allowFallbackMethodExceptionParameter()));
    }

    public BeforeRetryMethod getBeforeRetryMethod(InterceptionPoint point, FaultToleranceOperation operation) {
        return beforeRetryMethods.computeIfAbsent(point, ignored -> BeforeRetryMethod.create(operation));
    }
}
