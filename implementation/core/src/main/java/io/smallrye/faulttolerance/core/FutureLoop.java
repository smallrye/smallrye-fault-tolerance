package io.smallrye.faulttolerance.core;

import java.util.function.Function;
import java.util.function.Predicate;

// adapted from https://github.com/IBM/java-async-util/blob/master/asyncutil/src/main/java/com/ibm/asyncutil/iteration/AsyncTrampoline.java
// which is licensed under Apache License 2.0
final class FutureLoop<T> {
    private final FutureImpl<T> delegate = new FutureImpl<>();

    private final Predicate<T> condition;
    private final Function<T, Future<T>> iteration;

    private FutureLoop(Predicate<T> condition, Function<T, Future<T>> iteration) {
        this.condition = condition;
        this.iteration = iteration;
    }

    static <T> Future<T> loop(T initialValue, Predicate<T> condition, Function<T, Future<T>> iteration) {
        FutureLoop<T> loop = new FutureLoop<>(condition, iteration);
        loop.run(initialValue, null);
        return loop.delegate;
    }

    private void run(T value, Result<T> previousResult) {
        if (previousResult != null && previousResult.inProgress) {
            // synchronous iteration, set the result value and return to start new iteration in the old stack frame
            previousResult.value = value;
            return;
        }

        Result<T> currentResult = new Result<>();
        do {
            try {
                if (condition.test(value)) {
                    iteration.apply(value).then((val, error) -> {
                        if (error == null) {
                            run(val, currentResult);
                        } else {
                            delegate.completeWithError(error);
                        }
                    });
                } else {
                    delegate.complete(value);
                    return;
                }
            } catch (Throwable e) {
                delegate.completeWithError(e);
                return;
            }
        } while ((value = currentResult.resetValue()) != Result.NONE);
        currentResult.inProgress = false;
    }

    @SuppressWarnings("unchecked")
    private static class Result<T> {
        private static final Object NONE = new Object();

        boolean inProgress = true;
        T value = (T) NONE;

        T resetValue() {
            T result = value;
            value = (T) NONE;
            return result;
        }
    }
}
