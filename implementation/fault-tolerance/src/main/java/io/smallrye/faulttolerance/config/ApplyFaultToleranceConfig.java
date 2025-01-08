package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig(newConfigAllowed = false)
public interface ApplyFaultToleranceConfig extends ApplyFaultTolerance, ConfigDeclarativeOnly {
    @Override
    default void validate() {
        if (value().isEmpty()) {
            throw fail("value", "shouldn't be empty");
        }
    }
}
