package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.AsyncTypes;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface NonBlockingConfig extends NonBlocking, Config {
    @Override
    default void validate() {
        if (!AsyncTypes.isKnown(method().returnType)) {
            throw new FaultToleranceDefinitionException("Invalid @NonBlocking on " + method()
                    + ": must return " + AsyncValidation.describeKnownAsyncTypes());
        }
    }
}
