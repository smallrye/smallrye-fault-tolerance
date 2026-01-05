package io.smallrye.faulttolerance.apiimpl.basicconfig;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface CircuitBreakerConfig extends CircuitBreaker, Config {
    @Override
    default void validate() {
        if (delay() < 0) {
            throw fail("delay", "shouldn't be lower than 0");
        }
        if (requestVolumeThreshold() < 1) {
            throw fail("requestVolumeThreshold", "shouldn't be lower than 1");
        }
        if (failureRatio() < 0 || failureRatio() > 1) {
            throw fail("failureRation", "should be between 0 and 1");
        }
        if (successThreshold() < 1) {
            throw fail("successThreshold", "shouldn't be lower than 1");
        }
    }
}
