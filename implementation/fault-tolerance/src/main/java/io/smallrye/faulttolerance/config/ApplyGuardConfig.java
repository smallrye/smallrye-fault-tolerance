package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig
public interface ApplyGuardConfig extends ApplyGuard, ConfigDeclarativeOnly {
    @Override
    default void validate() {
        if (value().isEmpty()) {
            throw fail("value", "shouldn't be empty");
        }
    }
}
