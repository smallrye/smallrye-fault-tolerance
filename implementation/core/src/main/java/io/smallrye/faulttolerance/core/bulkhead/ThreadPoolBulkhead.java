package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CancellationEvent;

/**
 * Since this bulkhead is already executed on an extra thread, we don't have to
 * submit tasks to another executor or use an actual queue. We just limit
 * the task execution by semaphores. This kinda defeats the purpose of bulkheads,
 * but is the easiest way to implement them for {@code Future}. We do it properly
 * for {@code CompletionStage}, which is much more useful anyway.
 */
public class ThreadPoolBulkhead<V> extends BulkheadBase<Future<V>> {
    private final int queueSize;
    private final Semaphore capacitySemaphore;
    private final Semaphore workSemaphore;

    public ThreadPoolBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, int size, int queueSize) {
        super(description, delegate);
        this.queueSize = queueSize;
        this.capacitySemaphore = new Semaphore(size + queueSize);
        this.workSemaphore = new Semaphore(size);
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        if (capacitySemaphore.tryAcquire()) {
            ctx.fireEvent(BulkheadEvents.DecisionMade.ACCEPTED);
            ctx.fireEvent(BulkheadEvents.StartedWaiting.INSTANCE);

            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicReference<Thread> executingThread = new AtomicReference<>(Thread.currentThread());
            ctx.registerEventHandler(CancellationEvent.class, event -> {
                cancelled.set(true);
                if (event.interruptible) {
                    executingThread.get().interrupt();
                }
            });

            try {
                workSemaphore.acquire();
            } catch (InterruptedException e) {
                capacitySemaphore.release();
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
                workSemaphore.release();
                capacitySemaphore.release();
                ctx.fireEvent(BulkheadEvents.FinishedRunning.INSTANCE);
            }
        } else {
            ctx.fireEvent(BulkheadEvents.DecisionMade.REJECTED);
            throw bulkheadRejected();
        }
    }

    // only for tests
    int getQueueSize() {
        return Math.max(0, queueSize - capacitySemaphore.availablePermits());
    }
}
