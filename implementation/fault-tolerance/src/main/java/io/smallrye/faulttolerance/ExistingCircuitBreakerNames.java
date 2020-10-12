package io.smallrye.faulttolerance;

import javax.inject.Singleton;

/**
 * An integrator is allowed to provide a custom implementation of {@link ExistingCircuitBreakerNames}. The bean
 * should be {@link Singleton}, must be marked as alternative and selected globally for an application.
 */
public interface ExistingCircuitBreakerNames {
    boolean contains(String name);
}
