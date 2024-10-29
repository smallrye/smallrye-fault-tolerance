package io.smallrye.faulttolerance.core.metrics;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class GeneralMetricsEvents {
    public enum ExecutionFinished implements FaultToleranceEvent {
        VALUE_RETURNED(true),
        EXCEPTION_THROWN(false),
        ;

        public final boolean succeeded;

        ExecutionFinished(boolean succeeded) {
            this.succeeded = succeeded;
        }
    }
}
