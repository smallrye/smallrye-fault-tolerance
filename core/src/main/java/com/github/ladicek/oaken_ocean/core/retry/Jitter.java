package com.github.ladicek.oaken_ocean.core.retry;

/**
 * Implementations must be thread-safe.
 */
public interface Jitter {
    long generate();

    Jitter ZERO = new FixedJitter(0);
}
