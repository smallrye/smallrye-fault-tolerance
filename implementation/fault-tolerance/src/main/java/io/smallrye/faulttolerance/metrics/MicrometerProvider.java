package io.smallrye.faulttolerance.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.smallrye.faulttolerance.Enablement;
import io.smallrye.faulttolerance.ExecutorHolder;
import io.smallrye.faulttolerance.autoconfig.ConfigConstants;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicrometerRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

@Singleton
public class MicrometerProvider implements MetricsProvider {
    static final String DISABLED = ConfigConstants.PREFIX + "micrometer.disabled";

    private final boolean enabled;

    private final MeterRegistry registry;

    private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

    @Inject
    MicrometerProvider(
            // lazy for `CompoundMetricsProvider`
            Provider<MeterRegistry> registry,
            Enablement enablement,
            @ConfigProperty(name = DISABLED, defaultValue = "false") boolean micrometerDisabled,
            ExecutorHolder executorHolder) {
        this.enabled = enablement.metrics() && !micrometerDisabled;
        this.registry = registry.get();

        if (enabled) {
            Timer timer = executorHolder.getTimer();
            this.registry.gauge(MetricsConstants.TIMER_SCHEDULED, Collections.singletonList(Tag.of("id", "" + timer.getId())),
                    timer, Timer::countScheduledTasks);
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
                    ignored -> new MicrometerRecorder(registry, operation));
        } else {
            return MetricsRecorder.NOOP;
        }
    }
}
