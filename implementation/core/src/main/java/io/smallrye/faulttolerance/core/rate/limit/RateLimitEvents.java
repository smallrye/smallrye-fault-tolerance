package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.FaultToleranceEvent;

public class RateLimitEvents {
    public enum DecisionMade implements FaultToleranceEvent {
        PERMITTED(true),
        REJECTED(false),
        ;

        public final boolean permitted;

        DecisionMade(boolean permitted) {
            this.permitted = permitted;
        }
    }
}
