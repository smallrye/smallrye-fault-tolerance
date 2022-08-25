package io.smallrye.faulttolerance;

import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.invocation.AsyncSupportRegistry;

@Singleton
public class SpecCompatibility {
    private static final String PROPERTY = "smallrye.faulttolerance.mp-compatibility";

    private final boolean compatible;

    @Inject
    public SpecCompatibility(@ConfigProperty(name = PROPERTY, defaultValue = "true") boolean compatible) {
        this.compatible = compatible;
    }

    public static SpecCompatibility createFromConfig() {
        Boolean value = ConfigProvider.getConfig().getOptionalValue(PROPERTY, boolean.class).orElse(true);
        return new SpecCompatibility(value);
    }

    public boolean isOperationTrulyAsynchronous(FaultToleranceOperation operation) {
        boolean supported = AsyncSupportRegistry.isKnown(operation.getParameterTypes(), operation.getReturnType());

        if (compatible) {
            boolean hasAnnotation = operation.hasAsynchronous() || operation.hasAsynchronousNonBlocking()
                    || operation.hasBlocking() || operation.hasNonBlocking();
            return supported && hasAnnotation;
        } else {
            return supported;
        }
    }

    public boolean isOperationPseudoAsynchronous(FaultToleranceOperation operation) {
        // we don't have a non-compatible mode for methods that return `Future`,
        // we actively discourage using them
        boolean returnTypeMatches = Future.class.equals(operation.getReturnType());
        return returnTypeMatches && operation.hasAsynchronous();
    }

    public boolean isOperationTrulyOrPseudoAsynchronous(FaultToleranceOperation operation) {
        return isOperationTrulyAsynchronous(operation) || isOperationPseudoAsynchronous(operation);
    }

    public boolean inspectExceptionCauseChain() {
        return !compatible;
    }

    public boolean allowFallbackMethodExceptionParameter() {
        return !compatible;
    }
}
