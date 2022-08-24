package io.smallrye.faulttolerance.config;

import java.util.StringJoiner;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;

@AutoConfig
public interface AsynchronousNonBlockingConfig extends AsynchronousNonBlocking, Config {
    @Override
    default void validate() {
        Class<?>[] parameterTypes = method().parameterTypes;
        Class<?> returnType = method().returnType;
        if (AsyncSupportRegistry.isKnown(parameterTypes, returnType)) {
            return;
        }

        StringJoiner knownAsync = new StringJoiner(" or ");
        for (AsyncSupport<?, ?> asyncSupport : AsyncSupportRegistry.allKnown()) {
            knownAsync.add(asyncSupport.mustDescription());
        }
        throw new FaultToleranceDefinitionException("Invalid @AsynchronousNonBlocking on " + method()
                + ": must " + knownAsync);
    }
}
