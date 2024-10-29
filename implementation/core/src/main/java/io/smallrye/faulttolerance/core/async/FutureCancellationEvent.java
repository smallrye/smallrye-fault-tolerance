package io.smallrye.faulttolerance.core.async;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public enum FutureCancellationEvent implements FaultToleranceEvent {
    INTERRUPTIBLE(true),
    NONINTERRUPTIBLE(false);

    public final boolean interruptible;

    FutureCancellationEvent(boolean interruptible) {
        this.interruptible = interruptible;
    }
}
