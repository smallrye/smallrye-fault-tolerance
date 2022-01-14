package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ExceptionDecisionTest {
    @Test
    public void consideredFailure() {
        assertThat(ExceptionDecision.ALWAYS_FAILURE.isConsideredExpected(new Exception())).isFalse();
    }

    @Test
    public void consideredExpected() {
        assertThat(ExceptionDecision.ALWAYS_EXPECTED.isConsideredExpected(new Exception())).isTrue();
    }

    @Test
    public void unknown() {
        assertThat(ExceptionDecision.EMPTY.isConsideredExpected(new Exception())).isTrue();
    }

    @Test
    public void causeConsideredFailure() {
        ExceptionDecision decision = new ExceptionDecision(SetOfThrowables.create(TestException.class),
                SetOfThrowables.EMPTY, true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isFalse();
    }

    @Test
    public void causeConsideredExpected() {
        ExceptionDecision decision = new ExceptionDecision(SetOfThrowables.EMPTY,
                SetOfThrowables.create(TestException.class), true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isTrue();
    }

    @Test
    public void causeUnknown() {
        ExceptionDecision decision = new ExceptionDecision(SetOfThrowables.EMPTY, SetOfThrowables.EMPTY, true);

        assertThat(decision.isConsideredExpected(new Exception(new TestException()))).isTrue();
    }
}
