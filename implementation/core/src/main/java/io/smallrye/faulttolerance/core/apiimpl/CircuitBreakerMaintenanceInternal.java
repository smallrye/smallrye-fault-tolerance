package io.smallrye.faulttolerance.core.apiimpl;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;

public interface CircuitBreakerMaintenanceInternal extends CircuitBreakerMaintenance {
    void register(String circuitBreakerName, CircuitBreaker<?> circuitBreaker);
}
