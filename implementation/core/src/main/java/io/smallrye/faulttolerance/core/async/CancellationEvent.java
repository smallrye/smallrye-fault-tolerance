package io.smallrye.faulttolerance.core.async;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public enum CancellationEvent implements InvocationContextEvent {
    INTERRUPTIBLE(true),
    NONINTERRUPTIBLE(false);

    public final boolean interruptible;

    CancellationEvent(boolean interruptible) {
        this.interruptible = interruptible;
    }
}
