package io.smallrye.faulttolerance.core.metrics;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class GeneralMetricsEvents {
    public enum ExecutionFinished implements InvocationContextEvent {
        VALUE_RETURNED(true),
        EXCEPTION_THROWN(false),
        ;

        public final boolean succeeded;

        ExecutionFinished(boolean succeeded) {
            this.succeeded = succeeded;
        }
    }
}
