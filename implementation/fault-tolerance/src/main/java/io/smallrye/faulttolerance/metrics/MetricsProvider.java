package io.smallrye.faulttolerance.metrics;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;

public interface MetricsProvider {
    boolean isEnabled();

    MetricsRecorder create(FaultToleranceOperation operation);
}
