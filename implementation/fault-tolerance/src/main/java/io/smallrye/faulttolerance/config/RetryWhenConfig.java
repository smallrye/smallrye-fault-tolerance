package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface RetryWhenConfig extends RetryWhen, Config {
    @Override
    default void validate() {
    }
}
