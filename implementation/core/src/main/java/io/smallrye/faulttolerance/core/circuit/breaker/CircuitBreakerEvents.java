package io.smallrye.faulttolerance.core.circuit.breaker;

import io.smallrye.faulttolerance.core.InvocationContextEvent;

public class CircuitBreakerEvents {
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN,
    }

    public enum Result {
        SUCCESS,
        FAILURE,
        PREVENTED,
    }

    public enum StateTransition implements InvocationContextEvent {
        TO_CLOSED(State.CLOSED),
        TO_OPEN(State.OPEN),
        TO_HALF_OPEN(State.HALF_OPEN),
        ;

        public final State targetState;

        StateTransition(State targetState) {
            this.targetState = targetState;
        }
    }

    public enum Finished implements InvocationContextEvent {
        SUCCESS(Result.SUCCESS),
        FAILURE(Result.FAILURE),
        PREVENTED(Result.PREVENTED),
        ;

        public final Result result;

        Finished(Result result) {
            this.result = result;
        }
    }
}
