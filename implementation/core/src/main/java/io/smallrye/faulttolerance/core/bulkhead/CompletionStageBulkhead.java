package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageBulkhead<V> extends BulkheadBase<CompletionStage<V>> {
    private final ExecutorService executor;
    private final int queueSize;
    private final Semaphore workSemaphore;
    private final Semaphore capacitySemaphore;

    public CompletionStageBulkhead(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            ExecutorService executor, int size, int queueSize) {
        super(description, delegate);
        workSemaphore = new Semaphore(size);
        capacitySemaphore = new Semaphore(size + queueSize);
        this.queueSize = queueSize;
        this.executor = executor;
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        // TODO we shouldn't put tasks into the executor if they immediately block on workSemaphore,
        //  they should be put into some queue
        if (capacitySemaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);
            CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(ctx);
            executor.execute(task);
            return task.result;
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return failedStage(bulkheadRejected());
        }
    }

    // only for tests
    int getQueueSize() {
        return Math.max(0, queueSize - capacitySemaphore.availablePermits());
    }

    private class CompletionStageBulkheadTask implements Runnable {
        private final CompletableFuture<V> result = new CompletableFuture<>();
        private final InvocationContext<CompletionStage<V>> ctx;

        private CompletionStageBulkheadTask(InvocationContext<CompletionStage<V>> ctx) {
            this.ctx = ctx;
        }

        public void run() {
            try {
                workSemaphore.acquire();
            } catch (InterruptedException e) {
                // among other occasions, this also happens during shutdown
                capacitySemaphore.release();
                ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
                result.completeExceptionally(e);
                return;
            }

            CompletionStage<V> rawResult;
            ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                rawResult = delegate.apply(ctx);
                rawResult.whenComplete((value, error) -> {
                    releaseSemaphores();
                    ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(value);
                    }
                });
            } catch (Exception e) {
                releaseSemaphores();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
                result.completeExceptionally(e);
            }
        }
    }

    private void releaseSemaphores() {
        workSemaphore.release();
        capacitySemaphore.release();
    }
}
