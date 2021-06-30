package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface ExponentialBackoffConfig extends ExponentialBackoff, Config {
    @Override
    default void validate() {
        final String INVALID_EXPONENTIAL_BACKOFF_ON = "Invalid @ExponentialBackoff on ";

        if (factor() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_EXPONENTIAL_BACKOFF_ON + method()
                    + ": factor shouldn't be lower than 1");
        }
        if (maxDelay() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_EXPONENTIAL_BACKOFF_ON + method()
                    + ": maxDelay shouldn't be lower than 0");
        }
    }
}
