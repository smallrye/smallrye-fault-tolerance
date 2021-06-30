package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.util.Preconditions;
import io.smallrye.faulttolerance.core.util.Primitives;

/**
 * Always ignores the {@code cause} passed to {@code getInMillis}.
 */
public class FibonacciBackOff implements BackOff {
    private final long initialDelayInMillis;
    private final long maxDelayInMillis;
    private final Jitter jitter;

    private long lastDelayA;
    private long lastDelayB;

    public FibonacciBackOff(long initialDelayInMillis, Jitter jitter, long maxDelayInMillis) {
        this.initialDelayInMillis = Preconditions.check(initialDelayInMillis, initialDelayInMillis >= 0,
                "Initial delay must be >= 0");
        this.jitter = Preconditions.checkNotNull(jitter, "Jitter must be set");
        this.maxDelayInMillis = maxDelayInMillis <= 0 ? Long.MAX_VALUE : maxDelayInMillis;

        if (maxDelayInMillis > 0) {
            Preconditions.check(initialDelayInMillis, initialDelayInMillis < maxDelayInMillis,
                    "Initial delay must be < max delay");
        }
    }

    @Override
    public synchronized long getInMillis(Throwable cause) {
        if (lastDelayA == 0) {
            lastDelayA = initialDelayInMillis;
            return lastDelayA + jitter.generate();
        }

        if (lastDelayB == 0) {
            lastDelayB = initialDelayInMillis << 1; // 2 * initialDelayInMillis
            return lastDelayB + jitter.generate();
        }

        long delay = lastDelayA + lastDelayB;
        lastDelayA = lastDelayB;
        lastDelayB = delay;

        return Primitives.clamp(delay + jitter.generate(), 0, maxDelayInMillis);
    }
}
