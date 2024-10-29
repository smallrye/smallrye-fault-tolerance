package io.smallrye.faulttolerance.core.timeout;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class TimeoutEvents {
    public enum Started implements FaultToleranceEvent {
        INSTANCE
    }

    public enum Finished implements FaultToleranceEvent {
        NORMALLY(false),
        TIMED_OUT(true),
        ;

        public final boolean timedOut;

        Finished(boolean timedOut) {
            this.timedOut = timedOut;
        }
    }
}
