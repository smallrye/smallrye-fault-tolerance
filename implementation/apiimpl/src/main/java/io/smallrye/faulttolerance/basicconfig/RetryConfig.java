package io.smallrye.faulttolerance.basicconfig;

import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface RetryConfig extends Retry, Config {
    @Override
    default void validate() {
        if (maxRetries() < -1) {
            throw fail("maxRetries", "shouldn't be lower than -1");
        }
        if (delay() < 0) {
            throw fail("delay", "shouldn't be lower than 0");
        }
        if (maxDuration() < 0) {
            throw fail("maxDuration", "shouldn't be lower than 0");
        }
        long maxDuration = timeInMillis(maxDuration(), durationUnit());
        if (maxDuration > 0) {
            long delay = timeInMillis(delay(), delayUnit());
            if (maxDuration <= delay) {
                throw fail("maxDuration", "should be greater than delay");
            }
        }
        if (jitter() < 0) {
            throw fail("jitter", "shouldn't be lower than 0");
        }
    }
}
