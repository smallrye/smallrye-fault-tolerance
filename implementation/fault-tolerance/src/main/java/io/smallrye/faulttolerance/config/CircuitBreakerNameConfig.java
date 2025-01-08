package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig(configurable = false)
public interface CircuitBreakerNameConfig extends CircuitBreakerName, ConfigDeclarativeOnly {
    @Override
    default void validate() {
        if (value().isEmpty()) {
            throw fail("value", "must not be empty");
        }
    }
}
