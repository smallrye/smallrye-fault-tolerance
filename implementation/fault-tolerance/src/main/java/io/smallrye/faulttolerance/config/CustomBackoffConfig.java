package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface CustomBackoffConfig extends CustomBackoff, Config {
    @Override
    default void validate() {
    }
}
