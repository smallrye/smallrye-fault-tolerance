package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Utility methods for {@link CompletionStage} and {@link CompletableFuture}.
 */
public class CompletionStages {
    public static <T> void propagateCompletion(CompletionStage<T> from, CompletableFuture<T> to) {
        from.whenComplete((value, exception) -> {
            if (exception == null) {
                to.complete(value);
            } else {
                to.completeExceptionally(unwrap(exception));
            }
        });
    }

    private static Throwable unwrap(Throwable exception) {
        while (exception instanceof CompletionException) {
            exception = exception.getCause();
        }

        return exception;
    }
}
