package io.smallrye.faulttolerance.config;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface AsynchronousConfig extends Asynchronous, Config {
    @Override
    default void validate() {
        Class<?> returnType;
        try {
            returnType = method().returnType();
        } catch (ClassNotFoundException e) {
            throw new FaultToleranceDefinitionException(e);
        }

        if (!AsyncValidation.isAcceptableReturnType(returnType)) {
            throw new FaultToleranceDefinitionException("Invalid @Asynchronous on " + method()
                    + ": must return java.util.concurrent.Future or " + AsyncValidation.describeKnownAsyncTypes());
        }
    }
}
