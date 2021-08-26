package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.BeforeRetryAnnotation;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface BeforeRetryConfig extends BeforeRetryAnnotation, Config {

    @Override
    default void validate() {
    }
}
