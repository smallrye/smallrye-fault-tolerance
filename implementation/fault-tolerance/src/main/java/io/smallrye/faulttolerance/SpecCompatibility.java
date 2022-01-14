package io.smallrye.faulttolerance;

import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Singleton
public class SpecCompatibility {
    private final boolean compatible;

    @Inject
    public SpecCompatibility(
            @ConfigProperty(name = "smallrye.faulttolerance.mp-compatibility", defaultValue = "true") boolean compatible) {
        this.compatible = compatible;
    }

    public boolean isOperationTrulyAsynchronous(FaultToleranceOperation operation) {
        boolean returnTypeMatches = AsyncTypes.isKnown(operation.getReturnType());

        if (compatible) {
            boolean hasAnnotation = operation.hasAsynchronous() || operation.hasBlocking() || operation.hasNonBlocking();
            return returnTypeMatches && hasAnnotation;
        } else {
            return returnTypeMatches;
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
}
