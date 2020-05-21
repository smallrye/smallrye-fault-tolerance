package io.smallrye.faulttolerance.core.bulkhead;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class BulkheadEvents {
    public enum DecisionMade implements InvocationContextEvent {
        ACCEPTED(true),
        REJECTED(false),
        ;

        public final boolean accepted;

        DecisionMade(boolean accepted) {
            this.accepted = accepted;
        }
    }

    public enum StartedWaiting implements InvocationContextEvent {
        INSTANCE
    }

    public enum FinishedWaiting implements InvocationContextEvent {
        INSTANCE
    }

    public enum StartedRunning implements InvocationContextEvent {
        INSTANCE
    }

    public enum FinishedRunning implements InvocationContextEvent {
        INSTANCE
    }
}
