package io.smallrye.faulttolerance.core.fallback;

import java.util.function.Function;

import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.Future;

@FunctionalInterface
public interface FallbackFunction<T> extends Function<FailureContext, Future<T>> {
    FallbackFunction<?> IGNORE = ignored -> {
        throw new UnsupportedOperationException();
    };

    static <T> FallbackFunction<T> ignore() {
        return (FallbackFunction<T>) IGNORE;
    }
}
