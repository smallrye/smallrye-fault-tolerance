package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.bulkhead.BulkheadLogger.LOG;

import java.util.Deque;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.FutureCancellationEvent;

/**
 * Unified bulkhead that behaves in a semaphore fashion for synchronous executions
 * and in a thread pool fashion for asynchronous executions.
 * <p>
 * Implements a proper queue of tasks for async executions. When an async task
 * (that was previously allowed to enter) leaves the bulkhead, it will attempt
 * to take one task from the queue and execute it.
 * <p>
 * It also allows synchronous queueing, which is an implementation of a thread pool
 * bulkhead for pseudo-asynchronous invocations. Those are already offloaded to
 * an extra thread and so don't require proper queueing, just two semaphores.
 */
public class Bulkhead<V> implements FaultToleranceStrategy<V> {
    private final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final Deque<BulkheadTask> queue;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;
    private final boolean syncQueueing;

    // `syncQueueing` may only be enabled if this bulkhead is executed on an extra thread
    public Bulkhead(FaultToleranceStrategy<V> delegate, String description, int size, int queueSize, boolean syncQueueing) {
        this.delegate = delegate;
        this.description = description;
        this.queue = new ConcurrentLinkedDeque<>();
        this.capacitySemaphore = new Semaphore(size + queueSize, true);
        this.workSemaphore = new Semaphore(size, true);
        this.syncQueueing = syncQueueing;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        LOG.trace("Bulkhead started");
        try {
            if (ctx.isSync()) {
                if (syncQueueing) {
                    return applySyncWithQueueing(ctx);
                } else {
                    return applySync(ctx);
                }
            } else {
                return applyAsync(ctx);
            }
        } finally {
            LOG.trace("Bulkhead finished");
        }
    }

    private Future<V> applySync(FaultToleranceContext<V> ctx) {
        if (workSemaphore.tryAcquire()) {
            LOG.trace("Semaphore acquired, accepting task into bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                return delegate.apply(ctx);
            } finally {
                workSemaphore.release();
                LOG.trace("Semaphore released, task leaving bulkhead");
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
            }
        } else {
            LOG.debugOrTrace(description + " invocation prevented by bulkhead",
                    "Semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return Future.ofError(new BulkheadException(description + " rejected from bulkhead"));
        }
    }

    private Future<V> applySyncWithQueueing(FaultToleranceContext<V> ctx) {
        if (capacitySemaphore.tryAcquire()) {
            LOG.trace("Capacity semaphore acquired, accepting task into bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            AtomicBoolean cancellationInvalid = new AtomicBoolean(false);
            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicReference<Thread> executingThread = new AtomicReference<>(Thread.currentThread());
            ctx.registerEventHandler(FutureCancellationEvent.class, event -> {
                if (cancellationInvalid.get()) {
                    // in case of retries, multiple handlers of FutureCancellationEvent may be registered,
                    // need to make sure that a handler belonging to an older bulkhead task doesn't do anything
                    return;
                }

                if (LOG.isTraceEnabled()) {
                    LOG.tracef("Cancelling bulkhead task,%s interrupting executing thread",
                            (event.interruptible ? "" : " NOT"));
                }
                cancelled.set(true);
                if (event.interruptible) {
                    executingThread.get().interrupt();
                }
            });

            try {
                workSemaphore.acquire();
                LOG.trace("Work semaphore acquired, running task");
            } catch (InterruptedException e) {
                cancellationInvalid.set(true);

                capacitySemaphore.release();
                LOG.trace("Capacity semaphore released, task leaving bulkhead");
                ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
                return Future.ofError(new CancellationException());
            }

            ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                if (cancelled.get()) {
                    return Future.ofError(new CancellationException());
                }
                return delegate.apply(ctx);
            } finally {
                cancellationInvalid.set(true);

                workSemaphore.release();
                LOG.trace("Work semaphore released, task finished");
                capacitySemaphore.release();
                LOG.trace("Capacity semaphore released, task leaving bulkhead");
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
            }
        } else {
            LOG.debugOrTrace(description + " invocation prevented by bulkhead",
                    "Capacity semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return Future.ofError(new BulkheadException(description + " rejected from bulkhead"));
        }
    }

    private Future<V> applyAsync(FaultToleranceContext<V> ctx) {
        if (capacitySemaphore.tryAcquire()) {
            LOG.trace("Capacity semaphore acquired, accepting task into bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            BulkheadTask task = new BulkheadTask(ctx);
            queue.addLast(task);
            runQueuedTask();
            return task.result.future();
        } else {
            LOG.debugOrTrace(description + " invocation prevented by bulkhead",
                    "Capacity semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            return Future.ofError(new BulkheadException(description + " rejected from bulkhead"));
        }
    }

    private void runQueuedTask() {
        // it's enough to run just one queued task, because when that task finishes,
        // it will run another one, etc. etc.
        // this has performance implications (potentially fewer threads are utilized
        // than possible), but it currently makes the code easier to reason about
        BulkheadTask queuedTask = queue.pollFirst();
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

    // only for tests
    int getAvailableCapacityPermits() {
        return capacitySemaphore.availablePermits();
    }

    private class BulkheadTask {
        private final Completer<V> result = Completer.create();
        private final FaultToleranceContext<V> ctx;

        private BulkheadTask(FaultToleranceContext<V> ctx) {
            this.ctx = ctx;
        }

        public void run() {
            ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);

            Future<V> rawResult;
            try {
                rawResult = delegate.apply(ctx);
                rawResult.then((value, error) -> {
                    releaseSemaphores();
                    ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);

                    if (error == null) {
                        result.complete(value);
                    } else {
                        result.completeWithError(error);
                    }

                    runQueuedTask();
                });
            } catch (Exception e) {
                releaseSemaphores();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);

                result.completeWithError(e);

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
