package io.smallrye.faulttolerance.config;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;

@AutoConfig(newConfigAllowed = false)
public interface BlockingConfig extends Blocking, ConfigDeclarativeOnly {
    @Override
    default void validate() {
        // we don't validate the method return type, because the @Blocking annotation
        // may be present for another framework and we're supposed to ignore it
    }
}
