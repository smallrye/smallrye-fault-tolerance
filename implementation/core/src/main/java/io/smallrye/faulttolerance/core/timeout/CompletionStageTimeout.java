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
            TimeoutWatcher watcher, ExecutorService originalExecutor, MetricsRecorder metricsRecorder) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.originalExecutor = checkNotNull(originalExecutor, "original executor must be set");
        this.interruptCurrentThread = false;
    }

    // only for tests
    CompletionStageTimeout(FaultToleranceStrategy<CompletionStage<V>> delegate, String description, long timeoutInMillis,
            TimeoutWatcher watcher, ExecutorService originalExecutor, MetricsRecorder metricsRecorder,
            boolean interruptCurrentThread) {
        super(delegate, description, timeoutInMillis, watcher, metricsRecorder);
        this.originalExecutor = originalExecutor;
        this.interruptCurrentThread = interruptCurrentThread;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        CompletableFuture<V> result = new CompletableFuture<>();

        long start = System.nanoTime();

        AtomicBoolean completedWithTimeout = new AtomicBoolean(false);
        AtomicReference<CompletionStage<V>> runningTaskRef = new AtomicReference<>();
        Thread threadToInterrupt = interruptCurrentThread ? Thread.currentThread() : null;
        TimeoutExecution timeoutExecution = new TimeoutExecution(threadToInterrupt, timeoutInMillis, () -> {
            if (completedWithTimeout.compareAndSet(false, true)) {
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%% " + Thread.currentThread().getName() + " timeout from watcher thread");

                // completing the `result` means dependent stages (such as subsequent retry attempts, or fallback)
                // run immediately on the "current" thread
                // if we completed the `result` on the timeout watcher thread, it would mean arbitrary code
                // could execute on the timeout watcher thread, which is wrong
                originalExecutor.submit(() -> {
                    long end = System.nanoTime();
                    metricsRecorder.timeoutTimedOut(end - start);

                    CompletionStage<V> runningTask = runningTaskRef.get();
                    if (runningTask != null) {
                        // we pass `null` to the `TimeoutExecution` because we can't know in advance which thread to interrupt
                        // here, we compensate for that by interruptibly-cancelling the running task, if there's one
                        runningTask.toCompletableFuture().cancel(true);
                    }

                    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%% " + Thread.currentThread().getName() + " timeout completing on original executor");
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

            long end = System.nanoTime();

            if (timeoutExecution.hasTimedOut()) {
                if (completedWithTimeout.compareAndSet(false, true)) {
                    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%% " + Thread.currentThread().getName() + " timeout from async executor thread, completing on this thread");
                    metricsRecorder.timeoutTimedOut(end - start);
                    result.completeExceptionally(timeoutException(description));
                }
            } else if (exception != null) {
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%% " + Thread.currentThread().getName() + " failed without timeout");
                metricsRecorder.timeoutFailed(end - start);
                result.completeExceptionally(exception);
            } else {
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%% " + Thread.currentThread().getName() + " succeeded without timeout");
                metricsRecorder.timeoutSucceeded(end - start);
                result.complete(value);
            }
        });

        return result;
    }
}
