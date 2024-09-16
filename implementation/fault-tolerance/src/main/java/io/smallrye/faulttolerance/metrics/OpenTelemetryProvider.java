package io.smallrye.faulttolerance.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class OpenTelemetryProvider implements MetricsProvider {
    @Inject
    Meter meter;

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

        Timer timer = executorHolder.getTimer();
        Attributes attributes = Attributes.of(AttributeKey.stringKey("id"), "" + timer.getId());
        meter.upDownCounterBuilder(MetricsConstants.TIMER_SCHEDULED)
                .buildWithCallback(m -> m.record(timer.countScheduledTasks(), attributes));
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return cache.computeIfAbsent(operation.cacheKey(),
                    ignored -> new OpenTelemetryRecorder(meter, operation));
        } else {
            return MetricsRecorder.NOOP;
        }
    }
}
