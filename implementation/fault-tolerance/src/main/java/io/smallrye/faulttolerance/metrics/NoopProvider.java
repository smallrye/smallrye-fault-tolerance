package io.smallrye.faulttolerance.metrics;

import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

@Singleton
public class NoopProvider implements MetricsProvider {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public MetricsRecorder create(FaultToleranceOperation operation) {
        return MetricsRecorder.NOOP;
    }
}
