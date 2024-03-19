package io.smallrye.faulttolerance.metrics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import io.smallrye.faulttolerance.core.metrics.MeteredOperation;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.metrics.MetricsRecorder;
import io.smallrye.faulttolerance.core.metrics.MicroProfileMetricsRecorder;

@Singleton
public class MicroProfileMetricsProvider implements MetricsProvider {
    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    MetricRegistry registry;

    @Inject
    @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled", defaultValue = "true")
    boolean metricsEnabled;

    @Override
    public MetricsRecorder create(MeteredOperation operation) {
        if (metricsEnabled) {
            return new MicroProfileMetricsRecorder(registry, operation);
        } else {
            return MetricsRecorder.NOOP;
        }
    }

    @Override
    public boolean isEnabled() {
        return metricsEnabled;
    }
}
