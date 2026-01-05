package io.smallrye.faulttolerance.apiimpl.basicconfig;

import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface TimeoutConfig extends Timeout, Config {
    @Override
    default void validate() {
        if (value() < 0) {
            throw fail("value", "shouldn't be lower than 0");
        }
    }
}
