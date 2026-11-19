package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.retry.RetryLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.function.BiFunction;

public class ComputedBackOff implements BackOff {
    private final BiFunction<Object, Throwable, Duration> delayFunction;
    private final BackOff fallback;
    private final Jitter jitter;

    public ComputedBackOff(BiFunction<Object, Throwable, Duration> delayFunction, BackOff fallback, Jitter jitter) {
        this.delayFunction = checkNotNull(delayFunction, "Computed delay must be set");
        this.fallback = checkNotNull(fallback, "Fallback back-off must be set");
        this.jitter = checkNotNull(jitter, "Jitter must be set");
    }

    @Override
    public long getInMillis(Object result, Throwable cause) {
        Duration delay = null;
        try {
            delay = delayFunction.apply(result, cause);
        } catch (Exception e) {
            LOG.warn("Computed delay handler has thrown an exception", e);
        }
        if (delay == null) {
            return fallback.getInMillis(result, cause);
        }
        if (delay.isNegative()) {
            LOG.warn("Computed delay handler returned negative duration: " + delay
                    + ", falling back to configured backoff strategy");
            return fallback.getInMillis(result, cause);
        }
        return Math.max(0, delay.toMillis() + jitter.generate());
    }
}
