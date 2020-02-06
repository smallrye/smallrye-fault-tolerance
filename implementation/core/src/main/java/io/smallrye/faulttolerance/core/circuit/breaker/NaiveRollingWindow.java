package io.smallrye.faulttolerance.core.circuit.breaker;

final class NaiveRollingWindow implements RollingWindow {
    private final boolean[] failures;
    private final int failureThreshold;

    private int index = 0;
    private long counter = 0; // long to avoid int overflow

    @SuppressWarnings("UnnecessaryThis")
    NaiveRollingWindow(int size, int failureThreshold) {
        this.failures = new boolean[size];
        this.failureThreshold = failureThreshold;
    }

    @Override
    public synchronized boolean recordSuccess() {
        failures[nextIndex()] = false;
        return failureThresholdReached();
    }

    @Override
    public synchronized boolean recordFailure() {
        failures[nextIndex()] = true;
        return failureThresholdReached();
    }

    private int nextIndex() {
        counter++;

        int result = index;
        index = (index + 1) % failures.length;
        return result;
    }

    private boolean failureThresholdReached() {
        if (counter < failures.length) {
            return false;
        }

        int failures = 0;
        for (boolean failure : this.failures) {
            if (failure) {
                failures++;
            }
        }
        return failures >= failureThreshold;
    }
}
