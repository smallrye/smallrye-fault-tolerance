package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig(configurable = false)
public interface CircuitBreakerNameConfig extends CircuitBreakerName, Config {
    @Override
    default void validate() {
        if (value().isEmpty()) {
            throw new FaultToleranceDefinitionException("Invalid @CircuitBreakerName on " + method()
                    + ": circuit breaker name must not be empty");
        }
    }
}
