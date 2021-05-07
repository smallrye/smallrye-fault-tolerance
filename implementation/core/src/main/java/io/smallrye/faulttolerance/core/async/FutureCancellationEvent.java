package io.smallrye.faulttolerance.core.async;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public enum FutureCancellationEvent implements InvocationContextEvent {
    INTERRUPTIBLE(true),
    NONINTERRUPTIBLE(false);

    public final boolean interruptible;

    FutureCancellationEvent(boolean interruptible) {
        this.interruptible = interruptible;
    }
}
