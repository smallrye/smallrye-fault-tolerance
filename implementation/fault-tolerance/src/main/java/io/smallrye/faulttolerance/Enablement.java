package io.smallrye.faulttolerance;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.autoconfig.ConfigConstants;

@Singleton
public class Enablement {
    private final boolean ftEnabled;
    private final boolean metricsEnabled;

    @Inject
    Enablement(
            @ConfigProperty(name = ConfigConstants.PREFIX + "enabled") Optional<Boolean> newFtEnabled,
            @ConfigProperty(name = "MP_Fault_Tolerance_NonFallback_Enabled") Optional<Boolean> oldFtEnabled,
            @ConfigProperty(name = ConfigConstants.PREFIX + "metrics.enabled") Optional<Boolean> newMetricsEnabled,
            @ConfigProperty(name = "MP_Fault_Tolerance_Metrics_Enabled") Optional<Boolean> oldMetricsEnabled) {
        ftEnabled = newFtEnabled.orElse(oldFtEnabled.orElse(true));
        metricsEnabled = newMetricsEnabled.orElse(oldMetricsEnabled.orElse(true));
    }

    public boolean ft() {
        return ftEnabled;
    }

    public boolean metrics() {
        return metricsEnabled;
    }
}
