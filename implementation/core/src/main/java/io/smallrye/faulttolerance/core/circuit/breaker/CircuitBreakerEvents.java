package io.smallrye.faulttolerance.core.circuit.breaker;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class CircuitBreakerEvents {
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN,
    }

    public static class StateTransition implements InvocationContextEvent {
        public static final StateTransition TO_CLOSED = new StateTransition(State.CLOSED);
        public static final StateTransition TO_OPEN = new StateTransition(State.OPEN);
        public static final StateTransition TO_HALF_OPEN = new StateTransition(State.HALF_OPEN);

        public final State targetState;

        private StateTransition(State targetState) {
            this.targetState = targetState;
        }
    }
}
