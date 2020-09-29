package io.smallrye.faulttolerance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;

@Singleton
public class CircuitBreakerMaintenanceRegistry {
    private final ConcurrentMap<String, CircuitBreaker<?>> registry = new ConcurrentHashMap<>();

    void register(String circuitBreakerName, CircuitBreaker<?> circuitBreaker) {
        registry.putIfAbsent(circuitBreakerName, circuitBreaker);
    }

    @Produces
    @CircuitBreakerName("")
    @Dependent
    CircuitBreakerMaintenance produce(InjectionPoint injectionPoint) {
        String name = injectionPoint.getQualifiers()
                .stream()
                .filter(it -> it instanceof CircuitBreakerName)
                .map(it -> (CircuitBreakerName) it)
                .findAny()
                .orElseThrow(() -> new RuntimeException("missing @CircuitBreakerName at " + injectionPoint))
                .value();

        // note that this implementation assumes that circuit breakers are created lazily
        // and doesn't check for errors (such as duplicate circuit breaker names or injection
        // of an undefined circuit breaker); that should be checked elsewhere, preferrably during deployment

        return new CircuitBreakerMaintenance() {
            @Override
            public CircuitBreakerState currentState() {
                CircuitBreaker<?> circuitBreaker = registry.get(name);
                if (circuitBreaker == null) {
                    // if the circuit breaker doesn't exist yet, it's "closed" by definition
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
            public void reset() {
                CircuitBreaker<?> circuitBreaker = registry.get(name);
                if (circuitBreaker != null) {
                    // if the circuit breaker doesn't exist yet, "resetting it" is by definition a noop
                    circuitBreaker.reset();
                }
            }
        };
    }
}
