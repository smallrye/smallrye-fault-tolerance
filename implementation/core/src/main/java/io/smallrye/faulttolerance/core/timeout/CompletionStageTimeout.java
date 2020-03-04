package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageTimeout<V> extends Timeout<CompletionStage<V>> {
    // TODO only for unit tests, they need to be rewritten to always run CompletionStage* strategies asynchronously
    private final boolean interruptCurrentThread;

    public CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, MetricsRecorder metricsRecorder) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.interruptCurrentThread = false;
    }

    // only for tests
    CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, MetricsRecorder metricsRecorder, boolean interruptCurrentThread) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.interruptCurrentThread = interruptCurrentThread;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        AtomicReference<CompletionStage<V>> runningTaskRef = new AtomicReference<>();
        Thread threadToInterrupt = interruptCurrentThread ? Thread.currentThread() : null;
        TimeoutExecution timeoutExecution = new TimeoutExecution(threadToInterrupt, timeoutInMillis, () -> {
            result.completeExceptionally(timeoutException(description));
            CompletionStage<V> runningTask = runningTaskRef.get();
            if (runningTask != null) {
                // we pass `null` to the `TimeoutExecution` because we can't know in advance which thread to interrupt
                // here, we compensate for that by interruptibly-cancelling the running task, if there's one
                runningTask.toCompletableFuture().cancel(true);
            }
        });
        TimeoutWatch watch = watcher.schedule(timeoutExecution);
        long start = System.nanoTime();

        CompletionStage<V> originalResult;
        try {
            originalResult = delegate.apply(ctx);
            if (!interruptCurrentThread) {
                runningTaskRef.set(originalResult);
            }
        } catch (Exception e) {
            originalResult = failedStage(e);
        }

        originalResult.whenComplete((value, exception) -> {
            // if the execution timed out, this will be a noop
            //
            // this comes first, so that when the future is completed, the timeout watcher is already cancelled
            // (this isn't exactly needed, but makes tests easier to write)
            timeoutExecution.finish(watch::cancel);

            long end = System.nanoTime();

            if (timeoutExecution.hasTimedOut()) {
                metricsRecorder.timeoutTimedOut(end - start);
                result.completeExceptionally(timeoutException(description));
            } else if (exception != null) {
                metricsRecorder.timeoutFailed(end - start);
                result.completeExceptionally(exception);
            } else {
                metricsRecorder.timeoutSucceeded(end - start);
                result.complete(value);
            }
        });

        return result;
    }
}
