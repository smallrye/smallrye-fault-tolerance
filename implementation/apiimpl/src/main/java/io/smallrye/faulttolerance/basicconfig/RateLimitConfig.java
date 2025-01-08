package io.smallrye.faulttolerance.basicconfig;

import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface RateLimitConfig extends RateLimit, Config {
    @Override
    default void validate() {
        if (value() < 1) {
            throw fail("value", "shouldn't be lower than 1");
        }
        if (window() < 1) {
            throw fail("window", "shouldn't be lower than 1");
        }
        if (minSpacing() < 0) {
            throw fail("minSpacing", "shouldn't be lower than 0");
        }
    }
}
