package io.smallrye.faulttolerance.core.metrics;

public interface MetricsProvider {
    boolean isEnabled();

    MetricsRecorder create(MeteredOperation operation);
}
