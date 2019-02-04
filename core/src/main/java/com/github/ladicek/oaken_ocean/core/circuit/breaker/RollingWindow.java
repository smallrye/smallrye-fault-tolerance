package com.github.ladicek.oaken_ocean.core.circuit.breaker;

public interface RollingWindow {
    /**
     * Records a successful invocation and returns whether the failure threshold has been reached.
     */
    boolean recordSuccess();

    /**
     * Records a failed invocation and returns whether the failure threshold has been reached.
     */
    boolean recordFailure();

    static RollingWindow create(int size, int failureThreshold) {
        return new BitsetRollingWindow(size, failureThreshold);
    }
}
