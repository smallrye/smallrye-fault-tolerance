package io.smallrye.faulttolerance.core.fallback;

import java.util.concurrent.Executor;
import java.util.function.Function;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FailureContext;
import io.smallrye.faulttolerance.core.Future;

public final class ThreadOffloadFallbackFunction<T> implements FallbackFunction<T> {
    private final Function<FailureContext, Future<T>> delegate;
    private final Executor executor;

    public ThreadOffloadFallbackFunction(Function<FailureContext, Future<T>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public Future<T> apply(FailureContext ctx) {
        boolean hasRememberedExecutor = ctx.context.has(Executor.class);
        Executor executor = ctx.context.get(Executor.class, this.executor);

        Completer<T> result = Completer.create();
        if (hasRememberedExecutor) {
            executor.execute(() -> {
                try {
                    delegate.apply(ctx).then((value, error) -> {
                        executor.execute(() -> {
                            if (error == null) {
                                result.complete(value);
                            } else {
                                result.completeWithError(error);
                            }
                        });
                    });
                } catch (Exception e) {
                    result.completeWithError(e);
                }
            });
        } else {
            executor.execute(() -> {
                try {
                    delegate.apply(ctx).thenComplete(result);
                } catch (Exception e) {
                    result.completeWithError(e);
                }
            });
        }
        return result.future();
    }
}
