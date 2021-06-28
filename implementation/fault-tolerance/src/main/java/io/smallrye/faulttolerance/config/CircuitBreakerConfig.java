package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface CircuitBreakerConfig extends CircuitBreaker, Config {
    @Override
    default void validate() {
        final String INVALID_CIRCUIT_BREAKER_ON = "Invalid @CircuitBreaker on ";

        if (delay() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_CIRCUIT_BREAKER_ON + method()
                    + ": delay shouldn't be lower than 0");
        }
        if (requestVolumeThreshold() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_CIRCUIT_BREAKER_ON + method()
                    + ": requestVolumeThreshold shouldn't be lower than 1");
        }
        if (failureRatio() < 0 || failureRatio() > 1) {
            throw new FaultToleranceDefinitionException(INVALID_CIRCUIT_BREAKER_ON + method()
                    + ": failureRation should be between 0 and 1");
        }
        if (successThreshold() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_CIRCUIT_BREAKER_ON + method()
                    + ": successThreshold shouldn't be lower than 1");
        }
    }
}
