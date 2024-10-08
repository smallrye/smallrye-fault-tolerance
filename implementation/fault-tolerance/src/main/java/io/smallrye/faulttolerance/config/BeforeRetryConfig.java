package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface BeforeRetryConfig extends BeforeRetry, Config {
    default void validate() {
        if (!"".equals(methodName()) && !DEFAULT.class.equals(value())) {
            throw new FaultToleranceDefinitionException("Invalid @BeforeRetry on " + method()
                    + ": before retry handler class and before retry method can't be specified both at the same time");
        }
    }
}
