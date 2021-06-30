package io.smallrye.faulttolerance.core.retry;

import java.util.function.ToLongFunction;

import io.smallrye.faulttolerance.core.util.Preconditions;

public class CustomBackOff implements BackOff {
    private final ToLongFunction<Throwable> strategy;

    public CustomBackOff(ToLongFunction<Throwable> strategy) {
        this.strategy = Preconditions.checkNotNull(strategy, "Custom backoff strategy must be set");
    }

    @Override
    public long getInMillis(Throwable cause) {
        return strategy.applyAsLong(cause);
    }
}
