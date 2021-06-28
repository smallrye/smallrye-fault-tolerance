package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface TimeoutConfig extends Timeout, Config {
    @Override
    default void validate() {
        final String INVALID_TIMEOUT_ON = "Invalid @Timeout on ";

        if (value() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_TIMEOUT_ON + method()
                    + ": value shouldn't be lower than 0");
        }
    }
}
