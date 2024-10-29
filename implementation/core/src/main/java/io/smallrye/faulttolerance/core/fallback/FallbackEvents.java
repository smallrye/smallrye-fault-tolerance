package io.smallrye.faulttolerance.core.fallback;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class FallbackEvents {
    public enum Defined implements FaultToleranceEvent {
        INSTANCE
    }

    public enum Applied implements FaultToleranceEvent {
        INSTANCE
    }
}
