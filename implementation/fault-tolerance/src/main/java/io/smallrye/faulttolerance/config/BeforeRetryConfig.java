package io.smallrye.faulttolerance.config;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig
public interface BeforeRetryConfig extends BeforeRetry, ConfigDeclarativeOnly {
    default void validate() {
        if (!"".equals(methodName()) && !DEFAULT.class.equals(value())) {
            throw fail("before retry handler class and before retry method can't be specified both at the same time");
        }
    }
}
