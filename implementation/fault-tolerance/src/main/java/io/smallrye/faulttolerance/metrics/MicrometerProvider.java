package io.smallrye.faulttolerance.metrics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicrometerRecorder;

@Singleton
public class MicrometerProvider implements MetricsProvider {
    @Inject
    MeterRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return new MicrometerRecorder(registry, operation);
        } else {
            return MetricsRecorder.NOOP;
        }
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }
}
