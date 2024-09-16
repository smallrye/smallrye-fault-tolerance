package io.smallrye.faulttolerance.standalone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.OpenTelemetryRecorder;
import io.smallrye.faulttolerance.core.timer.Timer;

public final class OpenTelemetryAdapter implements MetricsAdapter {
    private final Meter meter;

    public OpenTelemetryAdapter(Meter meter) {
        this.meter = meter;
    }

    MetricsProvider createMetricsProvider(Timer timer) {
        Attributes attributes = Attributes.of(AttributeKey.stringKey("id"), "" + timer.getId());
        meter.upDownCounterBuilder(MetricsConstants.TIMER_SCHEDULED)
                .buildWithCallback(m -> m.record(timer.countScheduledTasks(), attributes));

        return new MetricsProvider() {
            private final Map<Object, MetricsRecorder> cache = new ConcurrentHashMap<>();

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public MetricsRecorder create(MeteredOperation operation) {
                return cache.computeIfAbsent(operation.cacheKey(),
                        ignored -> new OpenTelemetryRecorder(meter, operation));
            }
        };
    }
}
