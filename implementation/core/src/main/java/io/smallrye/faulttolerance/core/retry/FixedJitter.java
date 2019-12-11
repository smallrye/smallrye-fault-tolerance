package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.util.Preconditions;

/**
 * Always returns the same {@code value}.
 */
public class FixedJitter implements Jitter {
    private final long value;

    public FixedJitter(long value) {
        this.value = Preconditions.check(value, value >= 0, "Fixed jitter must be >= 0");
    }

    @Override
    public long generate() {
        return value;
    }
}
