package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.bulkhead.BulkheadLogger.LOG;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.FutureCancellationEvent;

/**
 * Thread pool style bulkhead for {@code Future} asynchronous executions.
 * <p>
 * Since this bulkhead is already executed on an extra thread, we don't have to
 * submit tasks to another executor or use an actual queue. We just limit
 * the task execution by semaphores. This kinda defeats the purpose of bulkheads,
 * but is the easiest way to implement them for {@code Future}. We do it properly
 * for {@code CompletionStage}, which is much more useful anyway.
 */
public class FutureThreadPoolBulkhead<V> extends BulkheadBase<Future<V>> {
    private final int queueSize;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public FutureThreadPoolBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, int size, int queueSize) {
        super(description, delegate);
        this.queueSize = queueSize;
        this.capacitySemaphore = new Semaphore(size + queueSize, true);
        this.workSemaphore = new Semaphore(size, true);
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        LOG.trace("ThreadPoolBulkhead started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("ThreadPoolBulkhead finished");
        }
    }

    private Future<V> doApply(InvocationContext<Future<V>> ctx) throws Exception {
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
                    LOG.trace("Cancelling bulkhead task," + (event.interruptible ? "" : " NOT")
                            + " interrupting executing thread");
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
                throw new CancellationException();
            }

            ctx.fireEvent(BulkheadEvents.FinishedWaiting.INSTANCE);
            ctx.fireEvent(BulkheadEvents.StartedRunning.INSTANCE);
            try {
                if (cancelled.get()) {
                    throw new CancellationException();
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
            LOG.trace("Capacity semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }

    // only for tests
    int getQueueSize() {
        return Math.max(0, queueSize - capacitySemaphore.availablePermits());
    }
}
