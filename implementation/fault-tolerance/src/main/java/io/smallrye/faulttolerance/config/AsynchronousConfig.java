package io.smallrye.faulttolerance.config;

import java.util.StringJoiner;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;

@AutoConfig
public interface AsynchronousConfig extends Asynchronous, Config {
    @Override
    default void validate() {
        Class<?>[] parameterTypes = method().parameterTypes;
        Class<?> returnType = method().returnType;
        if (Future.class.equals(returnType) || AsyncSupportRegistry.isKnown(parameterTypes, returnType)) {
            return;
        }

        StringJoiner knownAsync = new StringJoiner(" or ");
        for (AsyncSupport<?, ?> asyncSupport : AsyncSupportRegistry.allKnown()) {
            knownAsync.add(asyncSupport.mustDescription());
        }
        throw new FaultToleranceDefinitionException("Invalid @Asynchronous on " + method()
                + ": must return java.util.concurrent.Future or " + knownAsync);
    }
}
