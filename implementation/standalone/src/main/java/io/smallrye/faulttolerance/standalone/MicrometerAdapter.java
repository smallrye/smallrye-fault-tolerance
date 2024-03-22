package io.smallrye.faulttolerance.standalone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicrometerRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

public final class MicrometerAdapter implements MetricsAdapter {
    private final MeterRegistry registry;

    public MicrometerAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    MetricsProvider createMetricsProvider(Timer timer) {
        registry.gauge(MetricsConstants.TIMER_SCHEDULED, timer, Timer::countScheduledTasks);

        return new MetricsProvider() {
            private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public MetricsRecorder create(MeteredOperation operation) {
                return cache.computeIfAbsent(operation.cacheKey(),
                        ignored -> new MicrometerRecorder(registry, operation));
            }
        };
    }
}
