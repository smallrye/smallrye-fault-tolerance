package com.github.ladicek.oaken_ocean.core.timeout;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class CompletionStageTimeout<V> extends Timeout<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageTimeout(Callable<CompletionStage<V>> delegate, String description, long timeoutInMillis,
                                  TimeoutWatcher watcher, Executor executor) {
        super(delegate, description, timeoutInMillis, watcher);
        this.executor = executor;
    }

    // TODO interruptions?
    @Override
    public CompletionStage<V> call() {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            TimeoutExecution timeoutExecution = new TimeoutExecution(Thread.currentThread(), timeoutInMillis);
            TimeoutWatch watch = watcher.schedule(timeoutExecution);

            CompletionStage<V> originalResult;
            try {
                originalResult = delegate.call();
            } catch (Exception e) {
                result.completeExceptionally(timeoutExecution.hasTimedOut() ? timeoutException() : e);
                timeoutExecution.finish(watch::cancel);
                return;
            }

            originalResult.whenComplete((value, exception) -> {
                if (timeoutExecution.hasTimedOut()) {
                    result.completeExceptionally(timeoutException());
                } else if (exception != null) {
                    result.completeExceptionally(exception);
                } else {
                    result.complete(value);
                }

                // if the execution timed out, this will be a noop
                timeoutExecution.finish(watch::cancel);
            });
        });

        return result;
    }
}
