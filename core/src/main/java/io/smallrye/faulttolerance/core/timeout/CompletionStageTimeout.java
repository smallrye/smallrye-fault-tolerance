package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;

public class CompletionStageTimeout<V> extends SyncTimeout<CompletionStage<V>> {
    private final Executor executor;

    public CompletionStageTimeout(
            FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
            String description, long timeoutInMillis,
            TimeoutWatcher watcher, Executor executor, MetricsRecorder metricsRecorder) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.executor = executor;
    }

    // mstodo the offloading does not seem necessary, we could just kill the completion stage on timeout, I think
    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> context) {
        CompletableFuture<V> result = new CompletableFuture<>();

        executor.execute(() -> {
            TimeoutExecution timeoutExecution = new TimeoutExecution(Thread.currentThread(),
                    () -> result.completeExceptionally(timeoutException()), timeoutInMillis);
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
