package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

/**
 * This is supposed to be the <em>next to last</em> strategy in the chain (last being {@code Invocation}).
 * The common strategies never block and hence may be executed on the original thread.
 */
public class ThreadOffload<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final Executor executor;
    private final ThreadOffloadEnabled defaultEnabled;

    public ThreadOffload(FaultToleranceStrategy<V> delegate, Executor executor, boolean defaultEnabled) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
        this.defaultEnabled = new ThreadOffloadEnabled(defaultEnabled);
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        // required for `@ApplyGuard`
        boolean hasRememberedExecutor = ctx.has(Executor.class);
        if (!hasRememberedExecutor && !ctx.get(ThreadOffloadEnabled.class, defaultEnabled).value) {
            return delegate.apply(ctx);
        }

        LOG.trace("ThreadOffload started");
        try {
            Executor executor = ctx.get(Executor.class, this.executor);

            Completer<V> result = Completer.create();
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
        } finally {
            LOG.trace("ThreadOffload finished");
        }
    }
}
