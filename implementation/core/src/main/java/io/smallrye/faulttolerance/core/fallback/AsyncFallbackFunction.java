package io.smallrye.faulttolerance.core.fallback;

import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.smallrye.faulttolerance.core.FailureContext;

public final class AsyncFallbackFunction<T> implements Function<FailureContext, CompletionStage<T>> {
    private final Function<FailureContext, CompletionStage<T>> delegate;
    private final Executor executor;

    public AsyncFallbackFunction(Function<FailureContext, CompletionStage<T>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public CompletionStage<T> apply(FailureContext ctx) {
        boolean hasRememberedExecutor = ctx.invocationContext.has(Executor.class);
        Executor executor = ctx.invocationContext.get(Executor.class, this.executor);

        CompletableFuture<T> result = new CompletableFuture<>();
        if (hasRememberedExecutor) {
            executor.execute(() -> {
                try {
                    delegate.apply(ctx).whenComplete((value, error) -> {
                        executor.execute(() -> {
                            if (error == null) {
                                result.complete(value);
                            } else {
                                result.completeExceptionally(error);
                            }
                        });
                    });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        } else {
            executor.execute(() -> {
                try {
                    propagateCompletion(delegate.apply(ctx), result);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        }
        return result;
    }
}
