package io.smallrye.faulttolerance.core.fallback;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class FallbackEvents {
    public enum Defined implements InvocationContextEvent {
        INSTANCE
    }

    public enum Applied implements InvocationContextEvent {
        INSTANCE
    }
}
