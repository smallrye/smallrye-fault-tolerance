package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.bulkhead.BulkheadLogger.LOG;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

/**
 * Thread pool style bulkhead for {@code CompletionStage} asynchronous executions.
 * <p>
 * Implements a proper queue of tasks. When a task (that was previously allowed
 * to enter) leaves the bulkhead, it will attempt to take one task from
 * the queue and execute it.
 */
public class CompletionStageThreadPoolBulkhead<V> extends BulkheadBase<CompletionStage<V>> {
    private final Deque<CompletionStageBulkheadTask> queue;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public CompletionStageThreadPoolBulkhead(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            int size, int queueSize) {
        super(description, delegate);
        this.queue = new ConcurrentLinkedDeque<>();
        this.capacitySemaphore = new Semaphore(size + queueSize, true);
        this.workSemaphore = new Semaphore(size, true);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        LOG.trace("CompletionStageBulkhead started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("CompletionStageBulkhead finished");
        }
    }

    private CompletionStage<V> doApply(InvocationContext<CompletionStage<V>> ctx) {
        if (capacitySemaphore.tryAcquire()) {
            LOG.trace("Capacity semaphore acquired, accepting task into bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(ctx);
            queue.addLast(task);
            runQueuedTask();
            return task.result;
        } else {
            LOG.trace("Capacity semaphore not acquired, rejecting task from bulkhead");
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
                LOG.trace("Work semaphore acquired, running task");
                queuedTask.run();
            } else {
                LOG.trace("Work semaphore not acquired, putting task back to queue");
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
            LOG.trace("Work semaphore released, task finished");

            capacitySemaphore.release();
            LOG.trace("Capacity semaphore released, task leaving bulkhead");
        }
    }
}
