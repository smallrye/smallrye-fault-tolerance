package io.smallrye.faulttolerance.standalone;

import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

public final class NoopAdapter implements MetricsAdapter {
    public static final NoopAdapter INSTANCE = new NoopAdapter();

    private NoopAdapter() {
    }

    MetricsProvider createMetricsProvider() {
        return new MetricsProvider() {
            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public MetricsRecorder create(MeteredOperation operation) {
                return MetricsRecorder.NOOP;
            }
        };
    }
}
