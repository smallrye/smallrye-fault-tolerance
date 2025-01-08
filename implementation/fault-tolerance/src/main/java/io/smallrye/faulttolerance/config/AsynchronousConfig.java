package io.smallrye.faulttolerance.config;

import java.util.StringJoiner;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.ConfigDeclarativeOnly;
import io.smallrye.faulttolerance.core.invocation.AsyncSupport;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;

@AutoConfig
public interface AsynchronousConfig extends Asynchronous, ConfigDeclarativeOnly {
    @Override
    default void validate() {
        Class<?>[] parameterTypes = method().parameterTypes;
        Class<?> returnType = method().returnType;
        if (java.util.concurrent.Future.class.equals(returnType) || AsyncSupportRegistry.isKnown(parameterTypes, returnType)) {
            return;
        }

        StringJoiner knownAsync = new StringJoiner(" or ");
        for (AsyncSupport<?, ?> asyncSupport : AsyncSupportRegistry.allKnown()) {
            knownAsync.add(asyncSupport.mustDescription());
        }
        throw fail("must return java.util.concurrent.Future or " + knownAsync);
    }
}
