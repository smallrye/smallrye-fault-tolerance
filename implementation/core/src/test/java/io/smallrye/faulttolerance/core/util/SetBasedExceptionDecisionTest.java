package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SetBasedExceptionDecisionTest {
    @Test
    public void consideredFailure() {
        ExceptionDecision alwaysFailure = new SetBasedExceptionDecision(SetOfThrowables.ALL, SetOfThrowables.EMPTY, false);
        assertThat(alwaysFailure.isConsideredExpected(new Exception())).isFalse();
    }

    @Test
    public void consideredExpected() {
        ExceptionDecision alwaysExpected = new SetBasedExceptionDecision(SetOfThrowables.EMPTY, SetOfThrowables.ALL, false);
        assertThat(alwaysExpected.isConsideredExpected(new Exception())).isTrue();
    }

    @Test
    public void unknown() {
        ExceptionDecision empty = new SetBasedExceptionDecision(SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, false);
        assertThat(empty.isConsideredExpected(new Exception())).isTrue();
    }

    @Test
    public void causeConsideredFailure() {
        ExceptionDecision decision = new SetBasedExceptionDecision(SetOfThrowables.create(TestException.class),
                SetOfThrowables.EMPTY, true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isFalse();
    }

    @Test
    public void causeConsideredExpected() {
        ExceptionDecision decision = new SetBasedExceptionDecision(SetOfThrowables.EMPTY,
                SetOfThrowables.create(TestException.class), true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isTrue();
    }

    @Test
    public void causeUnknown() {
        ExceptionDecision decision = new SetBasedExceptionDecision(SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isTrue();
    }
}
