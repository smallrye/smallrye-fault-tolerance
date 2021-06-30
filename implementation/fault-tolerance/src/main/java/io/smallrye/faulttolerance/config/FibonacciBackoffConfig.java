package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface FibonacciBackoffConfig extends FibonacciBackoff, Config {
    @Override
    default void validate() {
        final String INVALID_FIBONACCI_BACKOFF_ON = "Invalid @FibonacciBackoff on ";

        if (maxDelay() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_FIBONACCI_BACKOFF_ON + method()
                    + ": maxDelay shouldn't be lower than 0");
        }
    }
}
