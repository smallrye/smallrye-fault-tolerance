package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import io.smallrye.faulttolerance.core.util.Primitives;

/**
 * Always ignores the {@code cause} passed to {@code getInMillis}.
 */
public class ExponentialBackOff implements BackOff {
    private final long initialDelayInMillis;
    private final long maxDelayInMillis;
    private final int factor;
    private final Jitter jitter;

    private long lastDelay;

    public ExponentialBackOff(long initialDelayInMillis, int factor, Jitter jitter, long maxDelayInMillis) {
        this.initialDelayInMillis = check(initialDelayInMillis, initialDelayInMillis >= 0,
                "Initial delay must be >= 0");
        this.factor = check(factor, factor >= 1, "Factor must be >= 1");
        this.jitter = checkNotNull(jitter, "Jitter must be set");
        this.maxDelayInMillis = maxDelayInMillis <= 0 ? Long.MAX_VALUE : maxDelayInMillis;

        if (maxDelayInMillis > 0) {
            check(initialDelayInMillis, initialDelayInMillis < maxDelayInMillis,
                    "Initial delay must be < max delay");
        }
    }

    @Override
    public synchronized long getInMillis(Throwable cause) {
        if (lastDelay == 0) {
            lastDelay = initialDelayInMillis;
            return lastDelay + jitter.generate();
        }

        lastDelay = lastDelay * factor;

        return Primitives.clamp(lastDelay + jitter.generate(), 0, maxDelayInMillis);
    }
}
