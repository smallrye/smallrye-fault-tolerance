package io.smallrye.faulttolerance.core.async;

import static io.smallrye.faulttolerance.core.async.AsyncLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.propagateCompletion;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

/**
 * Unlike {@link FutureExecution}, this is supposed to be the <em>next to last</em> strategy
 * in the chain (last being {@code Invocation}). To implement {@code CompletionStage}-based
 * {@code @Asynchronous} invocations, each synchronous strategy has a {@code CompletionStage}
 * counterpart that is used. The {@code CompletionStage} strategies never block and hence
 * may be executed on the original thread.
 */
public class CompletionStageExecution<V> implements FaultToleranceStrategy<CompletionStage<V>> {
    private final FaultToleranceStrategy<CompletionStage<V>> delegate;
    private final Executor executor;

    public CompletionStageExecution(FaultToleranceStrategy<CompletionStage<V>> delegate, Executor executor) {
        this.delegate = delegate;
        this.executor = checkNotNull(executor, "Executor must be set");
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageExecution started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageExecution finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        Executor executor = ctx.get(Executor.class);
        if (executor == null) {
            executor = this.executor;
        }

        CompletableFuture<V> result = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                propagateCompletion(delegate.apply(ctx), result);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }
}
