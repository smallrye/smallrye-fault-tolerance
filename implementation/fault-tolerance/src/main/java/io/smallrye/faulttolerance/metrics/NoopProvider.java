package io.smallrye.faulttolerance.metrics;

import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@Singleton
public class NoopProvider implements MetricsProvider {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        return MetricsRecorder.NOOP;
    }
}
