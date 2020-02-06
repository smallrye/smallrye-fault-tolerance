package io.smallrye.faulttolerance.core.circuit.breaker;

import java.util.BitSet;

final class BitsetRollingWindow implements RollingWindow {
    private final BitSet failures;
    private final int size;
    private final int failureThreshold;

    private int index = 0;
    private long counter = 0; // long to avoid int overflow

    @SuppressWarnings("UnnecessaryThis")
    BitsetRollingWindow(int size, int failureThreshold) {
        this.failures = new BitSet(size);
        this.size = size;
        this.failureThreshold = failureThreshold;
    }

    @Override
    public synchronized boolean recordSuccess() {
        failures.clear(nextIndex());
        return failureThresholdReached();
    }

    @Override
    public synchronized boolean recordFailure() {
        failures.set(nextIndex());
        return failureThresholdReached();
    }

    private int nextIndex() {
        counter++;

        int result = index;
        index = (index + 1) % size;
        return result;
    }

    private boolean failureThresholdReached() {
        if (counter < size) {
            return false;
        }

        return failures.cardinality() >= failureThreshold;
    }
}
