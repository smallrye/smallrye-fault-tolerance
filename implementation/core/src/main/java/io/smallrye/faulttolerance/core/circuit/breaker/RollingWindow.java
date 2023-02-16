package io.smallrye.faulttolerance.core.circuit.breaker;

public interface RollingWindow {
    /**
     * Records a successful invocation
     *
     * @return whether the failure threshold has been reached
     */
    boolean recordSuccess();

    /**
     * Records a failed invocation
     *
     * @return whether the failure threshold has been reached
     */
    boolean recordFailure();

    static RollingWindow create(int size, int failureThreshold) {
        return new BitsetRollingWindow(size, failureThreshold);
    }
}
