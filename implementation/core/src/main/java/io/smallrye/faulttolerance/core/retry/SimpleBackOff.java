package io.smallrye.faulttolerance.core.retry;

import io.smallrye.faulttolerance.core.util.Preconditions;

public class SimpleBackOff implements BackOff {
    private final long delayInMillis;
    private final Jitter jitter;

    public SimpleBackOff(long delayInMillis, Jitter jitter) {
        this.delayInMillis = Preconditions.check(delayInMillis, delayInMillis >= 0, "Delay must be >= 0");
        this.jitter = Preconditions.checkNotNull(jitter, "Jitter must be set");
    }

    @Override
    public long getInMillis() {
        return Math.max(0, delayInMillis + jitter.generate());
    }
}
