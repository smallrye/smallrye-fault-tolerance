package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface ApplyGuardConfig extends ApplyGuard, Config {
    @Override
    default void validate() {
        final String INVALID_APPLY_GUARD_ON = "Invalid @ApplyGuard on ";

        if (value().isEmpty()) {
            throw new FaultToleranceDefinitionException(INVALID_APPLY_GUARD_ON + method()
                    + ": value shouldn't be empty");
        }
    }
}
