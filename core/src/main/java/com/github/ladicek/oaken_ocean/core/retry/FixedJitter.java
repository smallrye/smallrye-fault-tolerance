package com.github.ladicek.oaken_ocean.core.retry;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;

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
