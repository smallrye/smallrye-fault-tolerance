package io.smallrye.faulttolerance.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicroProfileMetricsRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class MicroProfileMetricsProvider implements MetricsProvider {
    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    MetricRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Inject
    ExecutorHolder executorHolder;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        if (!metricsEnabled) {
            return;
        }

        Metadata metadata = Metadata.builder()
                .withName(MetricsConstants.TIMER_SCHEDULED)
                .withUnit(MetricUnits.NONE)
                .build();
        Timer timer = executorHolder.getTimer();
        registry.gauge(metadata, timer, Timer::countScheduledTasks, new Tag("id", "" + timer.getId()));
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return cache.computeIfAbsent(operation.cacheKey(),
                    ignored -> new MicroProfileMetricsRecorder(registry, operation));
        } else {
            return MetricsRecorder.NOOP;
        }
    }
}
