package io.smallrye.faulttolerance.core.timeout;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class TimeoutEvents {
    public enum Started implements InvocationContextEvent {
        INSTANCE
    }

    public enum Finished implements InvocationContextEvent {
        NORMALLY(false),
        TIMED_OUT(true),
        ;

        public final boolean timedOut;

        Finished(boolean timedOut) {
            this.timedOut = timedOut;
        }
    }
}
