package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.faulttolerance.AsyncTypes;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface BlockingConfig extends Blocking, Config {
    @Override
    default void validate() {
        if (!AsyncTypes.isKnown(method().returnType)) {
            throw new FaultToleranceDefinitionException("Invalid @Blocking on " + method()
                    + ": must return " + AsyncValidation.describeKnownAsyncTypes());
        }
    }
}
