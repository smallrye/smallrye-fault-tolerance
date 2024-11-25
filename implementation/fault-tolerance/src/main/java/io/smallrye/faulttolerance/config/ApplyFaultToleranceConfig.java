package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig(newConfigAllowed = false)
public interface ApplyFaultToleranceConfig extends ApplyFaultTolerance, Config {
    @Override
    default void validate() {
        final String INVALID_APPLY_FAULT_TOLERANCE_ON = "Invalid @ApplyFaultTolerance on ";

        if (value().isEmpty()) {
            throw new FaultToleranceDefinitionException(INVALID_APPLY_FAULT_TOLERANCE_ON + method()
                    + ": value shouldn't be empty");
        }
    }
}
