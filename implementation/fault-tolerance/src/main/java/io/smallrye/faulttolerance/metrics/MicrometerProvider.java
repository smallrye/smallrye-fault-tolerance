package io.smallrye.faulttolerance.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicrometerRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class MicrometerProvider implements MetricsProvider {
    @Inject
    MeterRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Inject
    ExecutorHolder executorHolder;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        registry.gauge(MetricsConstants.TIMER_SCHEDULED, executorHolder.getTimer(), Timer::countScheduledTasks);
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return cache.computeIfAbsent(operation.cacheKey(),
                    ignored -> new MicrometerRecorder(registry, operation));
        } else {
            return MetricsRecorder.NOOP;
        }
    }
}
