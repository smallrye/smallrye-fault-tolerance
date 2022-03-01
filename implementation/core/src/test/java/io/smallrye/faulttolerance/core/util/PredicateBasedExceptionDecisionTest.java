package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PredicateBasedExceptionDecisionTest {
    @Test
    public void consideredFailure() {
        ExceptionDecision alwaysFailure = new PredicateBasedExceptionDecision(ignored -> false);
        assertThat(alwaysFailure.isConsideredExpected(new Exception())).isFalse();
    }

    @Test
    public void consideredExpected() {
        ExceptionDecision alwaysExpected = new PredicateBasedExceptionDecision(ignored -> true);
        assertThat(alwaysExpected.isConsideredExpected(new Exception())).isTrue();
    }

    @Test
    public void causeConsideredFailure() {
        ExceptionDecision decision = new PredicateBasedExceptionDecision(e -> !(e.getCause() instanceof TestException));

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isFalse();
    }

    @Test
    public void causeConsideredExpected() {
        ExceptionDecision decision = new PredicateBasedExceptionDecision(e -> e.getCause() instanceof TestException);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isTrue();
    }
}
