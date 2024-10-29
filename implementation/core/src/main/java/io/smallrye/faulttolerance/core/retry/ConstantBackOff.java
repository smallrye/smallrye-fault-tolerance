package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.check;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

/**
 * Always ignores the {@code cause} passed to {@code getInMillis}.
 */
public class ConstantBackOff implements BackOff {
    private final long delayInMillis;
    private final Jitter jitter;

    public ConstantBackOff(long delayInMillis, Jitter jitter) {
        this.delayInMillis = check(delayInMillis, delayInMillis >= 0, "Delay must be >= 0");
        this.jitter = checkNotNull(jitter, "Jitter must be set");
    }

    @Override
    public long getInMillis(Throwable cause) {
        return Math.max(0, delayInMillis + jitter.generate());
    }
}
