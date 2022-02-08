package io.smallrye.faulttolerance;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.smallrye.faulttolerance.core.apiimpl.BasicCircuitBreakerMaintenanceImpl;

@Singleton
public class CircuitBreakerMaintenanceImpl extends BasicCircuitBreakerMaintenanceImpl {
    @Inject
    public CircuitBreakerMaintenanceImpl(ExistingCircuitBreakerNames existingCircuitBreakerNames) {
        super(existingCircuitBreakerNames::contains);
    }
}
