package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class CompletionStageTimeout<V> extends Timeout<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
                                  TimeoutWatcher watcher, Executor executor) {
        super(delegate, description, timeoutInMillis, watcher);
        this.executor = executor;
    }

    // TODO interruptions?
    @Override
    public CompletionStage<V> apply(Callable<CompletionStage<V>> target) throws Exception {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            TimeoutExecution timeoutExecution = new TimeoutExecution(Thread.currentThread(), timeoutInMillis);
            TimeoutWatch watch = watcher.schedule(timeoutExecution);

            CompletionStage<V> originalResult;
            try {
                originalResult = delegate.apply(target);
            } catch (Exception e) {
                // this comes first, so that when the future is completed, the timeout watcher is already cancelled
                // (this isn't exactly needed, but makes tests easier to write)
                timeoutExecution.finish(watch::cancel);

                result.completeExceptionally(timeoutExecution.hasTimedOut() ? timeoutException() : e);
                return;
            }

            originalResult.whenComplete((value, exception) -> {
                // if the execution timed out, this will be a noop
                //
                // this comes first, so that when the future is completed, the timeout watcher is already cancelled
                // (this isn't exactly needed, but makes tests easier to write)
                timeoutExecution.finish(watch::cancel);

                if (timeoutExecution.hasTimedOut()) {
                    result.completeExceptionally(timeoutException());
                } else if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(value);
                }
            });
        });

        return result;
    }
}
