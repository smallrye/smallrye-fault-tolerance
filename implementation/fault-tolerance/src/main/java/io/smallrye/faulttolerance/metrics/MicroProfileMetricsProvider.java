package io.smallrye.faulttolerance.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
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
    private final boolean metricsEnabled;

    private final MetricRegistry registry;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @Inject
    MicroProfileMetricsProvider(
            // lazy for `CompoundMetricsProvider`
            @RegistryType(type = MetricRegistry.Type.BASE) Provider<MetricRegistry> registry,
            @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true") boolean metricsEnabled,
            ExecutorHolder executorHolder) {
        this.metricsEnabled = metricsEnabled;
        this.registry = registry.get();

        Metadata metadata = Metadata.builder()
                .withName(MetricsConstants.TIMER_SCHEDULED)
                .withUnit(MetricUnits.NONE)
                .build();
        Timer timer = executorHolder.getTimer();
        this.registry.gauge(metadata, timer, Timer::countScheduledTasks, new Tag("id", "" + timer.getId()));
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
