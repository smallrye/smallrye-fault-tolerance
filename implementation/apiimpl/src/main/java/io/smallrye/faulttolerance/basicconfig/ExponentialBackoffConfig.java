package io.smallrye.faulttolerance.basicconfig;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface ExponentialBackoffConfig extends ExponentialBackoff, Config {
    @Override
    default void validate() {
        if (factor() < 1) {
            throw fail("factor", "shouldn't be lower than 1");
        }
        if (maxDelay() < 0) {
            throw fail("maxDelay", "shouldn't be lower than 0");
        }
    }
}
