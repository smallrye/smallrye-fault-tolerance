package io.smallrye.faulttolerance.metrics;

public enum MetricsIntegration {
    /**
     * Metrics integration using {@link MicroProfileMetricsProvider}.
     */
    MICROPROFILE_METRICS,
    /**
     * Metrics integration using {@link OpenTelemetryProvider}.
     */
    OPENTELEMETRY,
    /**
     * Metrics integration using {@link MicrometerProvider}.
     */
    MICROMETER,
    /**
     * Metrics will be disabled using {@link NoopProvider}.
     */
    NOOP,
}
