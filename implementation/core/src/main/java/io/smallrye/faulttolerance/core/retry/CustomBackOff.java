package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.function.ToLongFunction;

public class CustomBackOff implements BackOff {
    private final ToLongFunction<Throwable> strategy;

    public CustomBackOff(ToLongFunction<Throwable> strategy) {
        this.strategy = checkNotNull(strategy, "Custom backoff strategy must be set");
    }

    @Override
    public long getInMillis(Throwable cause) {
        return strategy.applyAsLong(cause);
    }
}
