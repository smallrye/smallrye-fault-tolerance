package com.github.ladicek.oaken_ocean.core.retry;

import static com.github.ladicek.oaken_ocean.core.util.Preconditions.check;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Provided a {@code maxJitter} value, generates random numbers that are greater than or equal to {@code -maxJitter}
 * and less than or equal to {@code maxJitter}.
 */
public class RandomJitter implements Jitter {
    private final long maxJitter;

    public RandomJitter(long maxJitter) {
        this.maxJitter = check(maxJitter, maxJitter > 0, "Max random jitter must be > 0");
    }

    @Override
    public long generate() {
        // lower bound is inclusive, upper bound is exclusive
        return ThreadLocalRandom.current().nextLong(-1 * maxJitter, maxJitter + 1);
    }
}
