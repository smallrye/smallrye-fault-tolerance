package io.smallrye.faulttolerance.apiimpl.basicconfig;

import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface FibonacciBackoffConfig extends FibonacciBackoff, Config {
    @Override
    default void validate() {
        if (maxDelay() < 0) {
            throw fail("maxDelay", "shouldn't be lower than 0");
        }
    }
}
