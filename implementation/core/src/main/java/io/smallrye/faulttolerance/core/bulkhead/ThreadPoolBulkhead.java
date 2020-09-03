package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CancellationEvent;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

public class ThreadPoolBulkhead<V> extends BulkheadBase<Future<V>> {
    private final ExecutorService executor;
    private final Deque<FutureBulkheadTask> queue;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public ThreadPoolBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description,
            ExecutorService executor, int size, int queueSize) {
        super(description, delegate);
        this.executor = executor;
        this.queue = new ConcurrentLinkedDeque<>();
        this.capacitySemaphore = new Semaphore(size + queueSize);
        this.workSemaphore = new Semaphore(size);
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        if (capacitySemaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            // the lambda here should really be created in the FutureBulkheadTask constructor,
            // so that ThreadPoolBulkhead is symmetric with CompletionStageBulkhead,
            // but that leads to a bytecode verification error on my OpenJDK 8
            FutureBulkheadTask task = new FutureBulkheadTask(() -> {
                ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
                ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
                try {
                    return delegate.apply(ctx);
                } finally {
                    ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
                }
            });

            ctx.registerEventHandler(CancellationEvent.class, ignored -> task.cancel());

            if (workSemaphore.tryAcquire()) {
                if (task.markRunning()) {
                    executor.execute(task);
                } else {
                    // cancelled, no need to do anything
                    workSemaphore.release();
                }
            } else {
                queue.addLast(task);
            }
            try {
                return task.get();
            } catch (InterruptedException e) {
                task.cancel(true);
                throw e; // TODO
            } catch (ExecutionException e) {
                throw sneakyThrow(e.getCause());
            }
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }

    // only for tests
    int getQueueSize() {
        return queue.size();
    }

    private class FutureBulkheadTask extends NamedFutureTask<Future<V>> {
        private static final int WAITING = 0;
        private static final int RUNNING = 1;
        private static final int CANCELLED = 2;

        private final AtomicInteger state = new AtomicInteger(WAITING);

        public FutureBulkheadTask(Callable<Future<V>> callable) {
            super("FutureBulkheadTask", callable);
        }

        public boolean markRunning() {
            return state.compareAndSet(WAITING, RUNNING);
        }

        @Override
        public void run() {
            try {
                super.run();
            } finally {
                releaseSemaphores();

                runQueuedTasks();
            }
        }

        public void cancel() {
            if (state.compareAndSet(WAITING, CANCELLED)) {
                releaseSemaphores();

                queue.remove(this);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return super.cancel(mayInterruptIfRunning);
        }

        private void releaseSemaphores() {
            if (this.state.get() == RUNNING) {
                workSemaphore.release();
            }
            capacitySemaphore.release();
        }

        private void runQueuedTasks() {
            FutureBulkheadTask queuedTask = queue.pollFirst();
            while (queuedTask != null) {
                if (workSemaphore.tryAcquire()) {
                    if (queuedTask.markRunning()) {
                        queuedTask.run(); // TODO this itself will again call runQueuedTasks, so do we need a loop?
                    } else {
                        // cancelled, no need to do anything
                        workSemaphore.release();
                    }
                    queuedTask = queue.poll();
                } else {
                    queue.addFirst(queuedTask);
                    break;
                }
            }
        }
    }
}
