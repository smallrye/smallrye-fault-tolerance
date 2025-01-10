package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;

/**
 * Always returns the same {@code value}.
 */
public class FixedJitter implements Jitter {
    private final long value;

    public FixedJitter(long value) {
        this.value = check(value, value >= 0, "Fixed jitter must be >= 0");
    }

    @Override
    public long generate() {
        return value;
    }
}
