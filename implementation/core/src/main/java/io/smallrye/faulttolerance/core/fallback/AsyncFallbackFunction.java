package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class AsyncFallbackFunction<T> implements FallbackFunction<CompletionStage<T>> {
    private final FallbackFunction<CompletionStage<T>> delegate;
    private final Executor executor;

    public AsyncFallbackFunction(FallbackFunction<CompletionStage<T>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public CompletionStage<T> call(FallbackContext<CompletionStage<T>> ctx) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                propagateCompletion(delegate.call(ctx), result);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }
}
