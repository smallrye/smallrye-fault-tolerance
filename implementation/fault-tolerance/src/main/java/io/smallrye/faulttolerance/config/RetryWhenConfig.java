package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig
public interface RetryWhenConfig extends RetryWhen, ConfigDeclarativeOnly {
    @Override
    default void validate() {
    }
}
