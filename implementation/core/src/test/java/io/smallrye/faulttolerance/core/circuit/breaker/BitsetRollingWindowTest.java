package io.smallrye.faulttolerance.core.circuit.breaker;

public class BitsetRollingWindowTest extends AbstractRollingWindowTest {
    @Override
    protected RollingWindow createRollingWindow(int size, int failureThreshold) {
        return new BitsetRollingWindow(size, failureThreshold);
    }
}
