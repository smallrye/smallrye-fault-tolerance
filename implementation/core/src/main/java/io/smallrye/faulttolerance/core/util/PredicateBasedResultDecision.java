package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.function.Predicate;

public class PredicateBasedResultDecision implements ResultDecision {
    private final Predicate<Object> isExpected;

    public PredicateBasedResultDecision(Predicate<Object> isExpected) {
        this.isExpected = checkNotNull(isExpected, "Result predicate must be set");
    }

    @Override
    public boolean isConsideredExpected(Object obj) {
        return isExpected.test(obj);
    }
}
