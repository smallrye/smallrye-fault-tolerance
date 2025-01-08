package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig
public interface CustomBackoffConfig extends CustomBackoff, ConfigDeclarativeOnly {
    @Override
    default void validate() {
    }
}
