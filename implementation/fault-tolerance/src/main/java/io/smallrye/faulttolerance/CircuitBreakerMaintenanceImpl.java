package io.smallrye.faulttolerance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;

@Singleton
public class CircuitBreakerMaintenanceImpl implements CircuitBreakerMaintenance {
    private final ConcurrentMap<String, CircuitBreaker<?>> registry = new ConcurrentHashMap<>();

    private final ExistingCircuitBreakerNames existingCircuitBreakerNames;

    @Inject
    public CircuitBreakerMaintenanceImpl(ExistingCircuitBreakerNames existingCircuitBreakerNames) {
        this.existingCircuitBreakerNames = existingCircuitBreakerNames;
    }

    void register(String circuitBreakerName, CircuitBreaker<?> circuitBreaker) {
        registry.putIfAbsent(circuitBreakerName, circuitBreaker);
    }

    @Override
    public CircuitBreakerState currentState(String name) {
        if (!existingCircuitBreakerNames.contains(name)) {
            throw new IllegalArgumentException("Circuit breaker '" + name + "' doesn't exist");
        }

        CircuitBreaker<?> circuitBreaker = registry.get(name);
        if (circuitBreaker == null) {
            // if the circuit breaker wasn't instantiated yet, it's "closed" by definition
            return CircuitBreakerState.CLOSED;
        }

        int currentState = circuitBreaker.currentState();
        switch (currentState) {
            case CircuitBreaker.STATE_CLOSED:
                return CircuitBreakerState.CLOSED;
            case CircuitBreaker.STATE_OPEN:
                return CircuitBreakerState.OPEN;
            case CircuitBreaker.STATE_HALF_OPEN:
                return CircuitBreakerState.HALF_OPEN;
            default:
                throw new IllegalStateException("Unknown circuit breaker state " + currentState);
        }
    }

    @Override
    public void reset(String name) {
        if (!existingCircuitBreakerNames.contains(name)) {
            throw new IllegalArgumentException("Circuit breaker '" + name + "' doesn't exist");
        }

        CircuitBreaker<?> circuitBreaker = registry.get(name);
        if (circuitBreaker != null) {
            // if the circuit breaker wasn't instantiated yet, "resetting it" is by definition a noop
            circuitBreaker.reset();
        }
    }

    @Override
    public void resetAll() {
        // circuit breakers that weren't instantiated yet don't have to be reset
        registry.values().forEach(CircuitBreaker::reset);
    }
}
