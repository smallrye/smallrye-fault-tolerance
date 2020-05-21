package io.smallrye.faulttolerance.core.async;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class CancellationEvent implements InvocationContextEvent {
    public static final CancellationEvent INSTANCE = new CancellationEvent();

    private CancellationEvent() {
        // avoid instantiation
    }
}
