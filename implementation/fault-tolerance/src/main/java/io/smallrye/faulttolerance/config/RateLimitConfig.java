package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface RateLimitConfig extends RateLimit, Config {
    @Override
    default void validate() {
        final String INVALID_RATE_LIMIT_ON = "Invalid @RateLimit on ";

        if (value() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_RATE_LIMIT_ON + method()
                    + ": value shouldn't be lower than 1");
        }
        if (window() < 1) {
            throw new FaultToleranceDefinitionException(INVALID_RATE_LIMIT_ON + method()
                    + ": window shouldn't be lower than 1");
        }
        if (minSpacing() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_RATE_LIMIT_ON + method()
                    + ": minSpacing shouldn't be lower than 0");
        }
    }
}
