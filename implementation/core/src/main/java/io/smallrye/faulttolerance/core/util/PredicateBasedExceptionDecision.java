package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.function.Predicate;

public class PredicateBasedExceptionDecision implements ExceptionDecision {
    private final Predicate<Throwable> isExpected;

    public PredicateBasedExceptionDecision(Predicate<Throwable> isExpected) {
        this.isExpected = checkNotNull(isExpected, "Exception predicate must be set");
    }

    @Override
    public boolean isConsideredExpected(Throwable e) {
        return isExpected.test(e);
    }
}
