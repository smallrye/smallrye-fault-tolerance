package io.smallrye.faulttolerance;

import jakarta.enterprise.context.Dependent;

import io.smallrye.faulttolerance.api.BeforeRetryHandler;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * An integrator is allowed to provide a custom implementation of {@link BeforeRetryHandlerProvider}. The bean should be
 * {@link Dependent}, must be marked as alternative and selected globally for an application.
 */
public interface BeforeRetryHandlerProvider {
    BeforeRetryHandler get(FaultToleranceOperation operation);
}
