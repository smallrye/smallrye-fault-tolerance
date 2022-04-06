package io.smallrye.faulttolerance.core.util;

import java.util.function.Predicate;

public class PredicateBasedExceptionDecision implements ExceptionDecision {
    private final Predicate<Throwable> isExpected;

    public PredicateBasedExceptionDecision(Predicate<Throwable> isExpected) {
        this.isExpected = isExpected;
    }

    @Override
    public boolean isConsideredExpected(Throwable e) {
        return isExpected.test(e);
    }
}
