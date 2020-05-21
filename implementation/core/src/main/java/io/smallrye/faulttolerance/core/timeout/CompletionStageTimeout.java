package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageTimeout<V> extends Timeout<CompletionStage<V>> {
    // in the real world (outside of tests), this must be the same executor that is used to execute the guarded method
    private final ExecutorService originalExecutor;

    // TODO only for unit tests, they need to be rewritten to always run CompletionStage* strategies asynchronously
    private final boolean interruptCurrentThread;

    public CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, ExecutorService originalExecutor) {
        super(delegate, description, timeoutInMillis, watcher);
        this.originalExecutor = checkNotNull(originalExecutor, "original executor must be set");
        this.interruptCurrentThread = false;
    }

    // only for tests
    CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, ExecutorService originalExecutor, boolean interruptCurrentThread) {
        super(delegate, description, timeoutInMillis, watcher);
        this.originalExecutor = originalExecutor;
        this.interruptCurrentThread = interruptCurrentThread;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        ctx.fireEvent(TimeoutEvents.Started.INSTANCE);

        AtomicBoolean completedWithTimeout = new AtomicBoolean(false);
        AtomicReference<CompletionStage<V>> runningTaskRef = new AtomicReference<>();
        Thread threadToInterrupt = interruptCurrentThread ? Thread.currentThread() : null;
        TimeoutExecution timeoutExecution = new TimeoutExecution(threadToInterrupt, timeoutInMillis, () -> {
            if (completedWithTimeout.compareAndSet(false, true)) {
                // completing the `result` means dependent stages (such as subsequent retry attempts, or fallback)
                // run immediately on the "current" thread
                // therefore, we run all the custom code here on the original executor, so that we don't exhaust
                // the timeout watcher thread pool
                originalExecutor.submit(() -> {
                    ctx.fireEvent(TimeoutEvents.Finished.TIMED_OUT);

                    CompletionStage<V> runningTask = runningTaskRef.get();
                    if (runningTask != null) {
                        // we pass `null` to the `TimeoutExecution` because we can't know in advance which thread to interrupt
                        // here, we compensate for that by interruptibly-cancelling the running task, if there's one
                        runningTask.toCompletableFuture().cancel(true);
                    }

                    result.completeExceptionally(timeoutException(description));
                });
            }
        });
        TimeoutWatch watch = watcher.schedule(timeoutExecution);

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

            if (timeoutExecution.hasTimedOut()) {
                if (completedWithTimeout.compareAndSet(false, true)) {
                    ctx.fireEvent(TimeoutEvents.Finished.TIMED_OUT);
                    result.completeExceptionally(timeoutException(description));
                }
            } else if (exception != null) {
                ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                result.completeExceptionally(exception);
            } else {
                ctx.fireEvent(TimeoutEvents.Finished.NORMALLY);
                result.complete(value);
            }
        });

        return result;
    }
}
