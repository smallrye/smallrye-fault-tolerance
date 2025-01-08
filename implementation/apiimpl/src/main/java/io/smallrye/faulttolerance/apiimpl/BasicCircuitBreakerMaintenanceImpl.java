package io.smallrye.faulttolerance.apiimpl;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreaker;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;
import io.smallrye.faulttolerance.core.util.Callbacks;

public class BasicCircuitBreakerMaintenanceImpl implements CircuitBreakerMaintenance {
    private final Set<String> knownNames = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, CircuitBreaker<?>> registry = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Collection<Consumer<CircuitBreakerState>>> stateChangeCallbacks = new ConcurrentHashMap<>();

    private final Predicate<String> circuitBreakerExists;

    public BasicCircuitBreakerMaintenanceImpl() {
        this.circuitBreakerExists = knownNames::contains;
    }

    protected BasicCircuitBreakerMaintenanceImpl(Predicate<String> additionalCircuitBreakerExists) {
        this.circuitBreakerExists = name -> additionalCircuitBreakerExists.test(name) || knownNames.contains(name);
    }

    public void registerName(String circuitBreakerName) {
        knownNames.add(circuitBreakerName);
    }

    public void register(String circuitBreakerName, CircuitBreaker<?> circuitBreaker) {
        knownNames.add(circuitBreakerName); // should not be necessary, just in case
        CircuitBreaker<?> previous = registry.putIfAbsent(circuitBreakerName, circuitBreaker);
        if (previous != null) {
            throw new IllegalStateException("Circuit breaker already exists: " + circuitBreakerName);
        }
    }

    @Override
    public CircuitBreakerState currentState(String name) {
        if (!circuitBreakerExists.test(name)) {
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
    public void onStateChange(String name, Consumer<CircuitBreakerState> callback) {
        if (!circuitBreakerExists.test(name)) {
            throw new IllegalArgumentException("Circuit breaker '" + name + "' doesn't exist");
        }

        stateChangeCallbacks.computeIfAbsent(name, ignored -> new CopyOnWriteArrayList<>())
                .add(Callbacks.wrap(callback));
    }

    public Consumer<CircuitBreakerEvents.StateTransition> stateTransitionEventHandler(String name) {
        return stateTransition -> {
            CircuitBreakerState targetState = stateTransition.targetState;
            Collection<Consumer<CircuitBreakerState>> callbacks = stateChangeCallbacks.get(name);
            if (callbacks != null) {
                for (Consumer<CircuitBreakerState> callback : callbacks) {
                    callback.accept(targetState);
                }
            }
        };
    }

    @Override
    public void reset(String name) {
        if (!circuitBreakerExists.test(name)) {
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
