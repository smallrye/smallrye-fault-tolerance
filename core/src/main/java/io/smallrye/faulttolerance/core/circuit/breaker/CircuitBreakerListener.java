package io.smallrye.faulttolerance.core.circuit.breaker;

/**
 * Implementations must be thread-safe and must not throw.
 */
// TODO not sure if this will be needed for real, currently only exists for tests
public interface CircuitBreakerListener {
    void succeeded();

    void failed();

    void rejected();
}
