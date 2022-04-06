package io.smallrye.faulttolerance;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.core.apiimpl.BasicCircuitBreakerMaintenanceImpl;

@Singleton
public class CircuitBreakerMaintenanceImpl extends BasicCircuitBreakerMaintenanceImpl {
    @Inject
    public CircuitBreakerMaintenanceImpl(ExistingCircuitBreakerNames existingCircuitBreakerNames) {
        super(existingCircuitBreakerNames::contains);
    }
}
