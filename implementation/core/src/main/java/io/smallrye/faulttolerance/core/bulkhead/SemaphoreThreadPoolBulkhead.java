package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.bulkhead.BulkheadLogger.LOG;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.FutureCancellationEvent;

/**
 * Assumes that this bulkhead is already executed on an extra thread.
 * Under that assumption, we don't have to submit tasks to another executor
 * or use an actual queue. We just limit the task execution by semaphores.
 */
public class SemaphoreThreadPoolBulkhead<V> extends BulkheadBase<V> {
    private final int queueSize;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public SemaphoreThreadPoolBulkhead(FaultToleranceStrategy<V> delegate, String description, int size, int queueSize) {
        super(description, delegate);
        this.queueSize = queueSize;
        this.capacitySemaphore = new Semaphore(size + queueSize, true);
        this.workSemaphore = new Semaphore(size, true);
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        LOG.trace("ThreadPoolBulkhead started");
        try {
            return doApply(ctx);
        } finally {
            LOG.trace("ThreadPoolBulkhead finished");
        }
    }

    private V doApply(InvocationContext<V> ctx) throws Exception {
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
            LOG.debugOrTrace(description + " invocation prevented by bulkhead",
                    "Capacity semaphore not acquired, rejecting task from bulkhead");
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }

    // only for tests
    int getQueueSize() {
        return Math.max(0, queueSize - capacitySemaphore.availablePermits());
    }
}
