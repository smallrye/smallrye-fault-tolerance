package com.github.ladicek.oaken_ocean.core.timeout;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class CompletionStageTimeout<V> extends SyncTimeout<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
                                  String description, long timeoutInMillis,
                                  TimeoutWatcher watcher, Executor executor, MetricsRecorder metricsRecorder) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.executor = executor;
    }


    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> context) throws Exception {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            TimeoutExecution timeoutExecution =
                  new TimeoutExecution(Thread.currentThread(), () -> result.completeExceptionally(timeoutException()), timeoutInMillis);
            TimeoutWatch watch = watcher.schedule(timeoutExecution);

            CompletionStage<V> originalResult;
            try {
                originalResult = delegate.apply(context);
            } catch (Exception e) {
                // this comes first, so that when the future is completed, the timeout watcher is already cancelled
                // (this isn't exactly needed, but makes tests easier to write)
                timeoutExecution.finish(watch::cancel);
                if (!result.isDone()) {
                    result.completeExceptionally(timeoutExecution.hasTimedOut() ? timeoutException() : e);
                }
                return;
            }

            if (result.isDone()) {
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
