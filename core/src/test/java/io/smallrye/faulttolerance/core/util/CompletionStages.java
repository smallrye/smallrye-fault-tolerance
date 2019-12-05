package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Utility methods for {@link CompletionStage}.
 */
public class CompletionStages {
    public static <T> CompletionStage<T> completedStage(T value) {
        return CompletableFuture.completedFuture(value);
    }

    public static <T> CompletionStage<T> failedStage(Throwable exception) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(exception);
        return result;
    }
}
