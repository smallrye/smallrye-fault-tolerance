package io.smallrye.faulttolerance.config;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface NonBlockingConfig extends NonBlocking, Config {
    @Override
    default void validate() {
        // we don't validate the method return type, because the @NonBlocking annotation
        // may be present for another framework and we're supposed to ignore it
    }
}
