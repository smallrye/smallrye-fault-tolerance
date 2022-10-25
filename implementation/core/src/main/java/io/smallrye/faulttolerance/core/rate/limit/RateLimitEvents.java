package io.smallrye.faulttolerance.core.rate.limit;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class RateLimitEvents {
    public enum DecisionMade implements InvocationContextEvent {
        PERMITTED(true),
        REJECTED(false),
        ;

        public final boolean permitted;

        DecisionMade(boolean permitted) {
            this.permitted = permitted;
        }
    }
}
