package io.smallrye.faulttolerance.core.bulkhead;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class BulkheadEvents {
    public enum DecisionMade implements FaultToleranceEvent {
        ACCEPTED(true),
        REJECTED(false),
        ;

        public final boolean accepted;

        DecisionMade(boolean accepted) {
            this.accepted = accepted;
        }
    }

    public enum StartedWaiting implements FaultToleranceEvent {
        INSTANCE
    }

    public enum FinishedWaiting implements FaultToleranceEvent {
        INSTANCE
    }

    public enum StartedRunning implements FaultToleranceEvent {
        INSTANCE
    }

    public enum FinishedRunning implements FaultToleranceEvent {
        INSTANCE
    }
}
