package io.smallrye.faulttolerance.core.util;

public interface ExceptionDecision {
    boolean isConsideredExpected(Throwable e);

    ExceptionDecision ALWAYS_EXPECTED = ignored -> true;
    ExceptionDecision ALWAYS_FAILURE = ignored -> false;
}
