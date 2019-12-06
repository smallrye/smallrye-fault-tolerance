package io.smallrye.faulttolerance.core.circuit.breaker;

/**
 * Implementations must be thread-safe and must not throw.
 */
// MSTODO replace with metrics
public interface CircuitBreakerListener {
    void succeeded();

    void failed();

    void rejected();
}
