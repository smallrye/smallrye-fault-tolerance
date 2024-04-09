package io.smallrye.faulttolerance.core.util;

public interface ResultDecision {
    boolean isConsideredExpected(Object obj);

    ResultDecision ALWAYS_EXPECTED = ignored -> true;
    ResultDecision ALWAYS_FAILURE = ignored -> false;
}
