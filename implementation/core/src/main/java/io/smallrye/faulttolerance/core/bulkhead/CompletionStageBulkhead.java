package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public class CompletionStageBulkhead<V> extends BulkheadBase<CompletionStage<V>> {
    private final ExecutorService executor;
    private final Deque<CompletionStageBulkheadTask> queue;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public CompletionStageBulkhead(FaultToleranceStrategy<CompletionStage<V>> delegate, String description,
            ExecutorService executor, int size, int queueSize) {
        super(description, delegate);
        this.executor = executor;
        this.queue = new ConcurrentLinkedDeque<>();
        this.capacitySemaphore = new Semaphore(size + queueSize);
        this.workSemaphore = new Semaphore(size);
    }

    @Override
    public CompletionStage<V> apply(InvocationContext<CompletionStage<V>> ctx) {
        if (capacitySemaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(ctx);
            if (workSemaphore.tryAcquire()) {
                // TODO eventually, we should just `task.run()` here, because for CompletionStage,
                //  Invocation moves the execution to a thread pool, so we don't have to do it here
                //  (except that's just in "real" code, we need to review and adjust unit tests)
                executor.execute(task);
            } else {
                queue.addLast(task);
            }
            return task.result;
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return failedStage(bulkheadRejected());
        }
    }

    // only for tests
    int getQueueSize() {
        return queue.size();
    }

    private class CompletionStageBulkheadTask implements Runnable {
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

                    runQueuedTasks();
                });
            } catch (Exception e) {
                releaseSemaphores();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
                result.completeExceptionally(e);

                runQueuedTasks();
            }
        }

        private void releaseSemaphores() {
            workSemaphore.release();
            capacitySemaphore.release();
        }

        private void runQueuedTasks() {
            CompletionStageBulkheadTask queuedTask = queue.pollFirst();
            while (queuedTask != null) {
                if (workSemaphore.tryAcquire()) {
                    queuedTask.run(); // TODO this itself will again call runQueuedTasks, so do we need a loop?
                    queuedTask = queue.poll();
                } else {
                    queue.addFirst(queuedTask);
                    break;
                }
            }
        }
    }
}
