package io.smallrye.faulttolerance.core;

import java.util.concurrent.atomic.AtomicReference;
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

    private static final Object ITERATION_IN_PROGRESS = new Object();
    private static final Object ITERATION_DONE = new Object();

    private void run(T value, AtomicReference<Object> previousResult) {
        if (previousResult != null && previousResult.compareAndSet(ITERATION_IN_PROGRESS, value)) {
            // synchronous iteration, set the result value and return to start new iteration in the old stack frame
            return;
        }

        AtomicReference<Object> currentResult = new AtomicReference<>(ITERATION_IN_PROGRESS);
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

            Object claimed = currentResult.compareAndExchange(ITERATION_IN_PROGRESS, ITERATION_DONE);
            if (claimed == ITERATION_IN_PROGRESS) {
                return;
            }
            value = (T) claimed;
            currentResult.set(ITERATION_IN_PROGRESS);
        } while (true);
    }
}
