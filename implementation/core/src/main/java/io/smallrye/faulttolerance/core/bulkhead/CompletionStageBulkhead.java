package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageBulkhead<V> extends BulkheadBase<CompletionStage<V>> {
    private final Deque<CompletionStageBulkheadTask> queue;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public CompletionStageBulkhead(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            int size, int queueSize) {
        super(description, delegate);
        this.queue = new ConcurrentLinkedDeque<>();
        this.capacitySemaphore = new Semaphore(size + queueSize, true);
        this.workSemaphore = new Semaphore(size, true);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        if (capacitySemaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(ctx);
            queue.addLast(task);
            runQueuedTask();
            return task.result;
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return failedStage(bulkheadRejected());
        }
    }

    private void runQueuedTask() {
        // it's enough to run just one queued task, because when that task finishes,
        // it will run another one, etc. etc.
        // this has performance implications (potentially less threads are utilized
        // than possible), but it currently makes the code easier to reason about
        CompletionStageBulkheadTask queuedTask = queue.pollFirst();
        if (queuedTask != null) {
            if (workSemaphore.tryAcquire()) {
                queuedTask.run();
            } else {
                queue.addFirst(queuedTask);
            }
        }
    }

    // only for tests
    int getQueueSize() {
        return queue.size();
    }

    private class CompletionStageBulkheadTask {
        private final CompletableFuture<V> result = new CompletableFuture<>();
        private final InvocationContext<CompletionStage<V>> ctx;

        private CompletionStageBulkheadTask(InvocationContext<CompletionStage<V>> ctx) {
            this.ctx = ctx;
        }

        public void run() {
            ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);

            CompletionStage<V> rawResult;
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

                    runQueuedTask();
                });
            } catch (Exception e) {
                releaseSemaphores();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
                result.completeExceptionally(e);

                runQueuedTask();
            }
        }

        private void releaseSemaphores() {
            workSemaphore.release();
            capacitySemaphore.release();
        }
    }
}
