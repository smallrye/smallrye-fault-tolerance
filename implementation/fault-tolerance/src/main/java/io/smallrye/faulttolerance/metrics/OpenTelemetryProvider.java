package io.smallrye.faulttolerance.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.faulttolerance.Enablement;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.autoconfig.ConfigConstants;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class OpenTelemetryProvider implements MetricsProvider {
    static final String DISABLED = ConfigConstants.PREFIX + "opentelemetry.disabled";

    private final boolean enabled;

    private final Meter meter;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @Inject
    OpenTelemetryProvider(
            // lazy for `CompoundMetricsProvider`
            Provider<Meter> meter,
            Enablement enablement,
            @ConfigProperty(name = DISABLED, defaultValue = "false") boolean openTelemetryDisabled,
            ExecutorHolder executorHolder) {
        this.enabled = enablement.metrics() && !openTelemetryDisabled;
        this.meter = meter.get();

        if (enabled) {
            Timer timer = executorHolder.getTimer();
            Attributes attributes = Attributes.of(AttributeKey.stringKey("id"), "" + timer.getId());
            this.meter.upDownCounterBuilder(MetricsConstants.TIMER_SCHEDULED)
                    .buildWithCallback(m -> m.record(timer.countScheduledTasks(), attributes));
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (enabled) {
            return cache.computeIfAbsent(operation.cacheKey(),
                    ignored -> new OpenTelemetryRecorder(meter, operation));
        } else {
            return MetricsRecorder.NOOP;
        }
    }
}
