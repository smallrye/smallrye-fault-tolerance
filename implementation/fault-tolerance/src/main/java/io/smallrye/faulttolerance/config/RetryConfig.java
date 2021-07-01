package io.smallrye.faulttolerance.config;

import java.time.Duration;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface RetryConfig extends Retry, Config {
    @Override
    default void validate() {
        final String INVALID_RETRY_ON = "Invalid @Retry on ";

        if (maxRetries() < -1) {
            throw new FaultToleranceDefinitionException(INVALID_RETRY_ON + method()
                    + ": maxRetries shouldn't be lower than -1");
        }
        if (delay() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_RETRY_ON + method()
                    + ": delay shouldn't be lower than 0");
        }
        if (maxDuration() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_RETRY_ON + method()
                    + ": maxDuration shouldn't be lower than 0");
        }
        long maxDuration = Duration.of(maxDuration(), durationUnit()).toMillis();
        if (maxDuration > 0) {
            long delay = Duration.of(delay(), delayUnit()).toMillis();
            if (maxDuration <= delay) {
                throw new FaultToleranceDefinitionException(INVALID_RETRY_ON + method()
                        + ": maxDuration should be greater than delay");
            }
        }
        if (jitter() < 0) {
            throw new FaultToleranceDefinitionException(INVALID_RETRY_ON + method()
                    + ": jitter shouldn't be lower than 0");
        }
    }
}
