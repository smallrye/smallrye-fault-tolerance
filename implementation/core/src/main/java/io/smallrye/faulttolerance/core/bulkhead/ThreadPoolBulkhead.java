package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.async.CancellationEvent;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

/**
 * The implementation intentionally doesn't leverage an executor with
 * a limited queue but limits the task intake with Semaphore
 * <br>
 * Proper removal from executor's queue is not possible for MicroProfile Context Propagation.
 * Also, the cost of removal from a linked queue is large, and the current solution should
 * should perform much better.
 *
 * @param <V> type of the Future result
 */
public class ThreadPoolBulkhead<V> extends BulkheadBase<Future<V>> {
    private final ExecutorService executor;
    private final Semaphore capacitySemaphore;
    private final int queueSize;

    public ThreadPoolBulkhead(
            FaultToleranceStrategy<Future<V>> delegate,
            String description,
            ExecutorService executor,
            int size, int queueSize,
            MetricsRecorder recorder) {
        super(description, delegate, recorder);
        capacitySemaphore = new Semaphore(size + queueSize);
        this.queueSize = queueSize;
        this.executor = executor;
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        long timeEnqueued = System.nanoTime();
        if (capacitySemaphore.tryAcquire()) {
            BulkheadTask task = new BulkheadTask("ThreadPoolBulkhead", () -> {
                long startTime = System.nanoTime();
                recorder.bulkheadQueueLeft(startTime - timeEnqueued);
                recorder.bulkheadEntered();
                try {
                    return delegate.apply(ctx);
                } finally {
                    recorder.bulkheadLeft(System.nanoTime() - startTime);
                }
            });
            ctx.registerEventHandler(CancellationEvent.class, ignored -> task.cancel());
            executor.execute(task);
            recorder.bulkheadQueueEntered();

            try {
                return task.get();
            } catch (InterruptedException e) {
                task.cancel(true);
                throw e; // TODO
            } catch (ExecutionException e) {
                throw sneakyThrow(e.getCause());
            }
        } else {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }

    // only for testing
    int getQueueSize() {
        return Math.max(0, queueSize - capacitySemaphore.availablePermits());
    }

    private class BulkheadTask extends NamedFutureTask<Future<V>> {
        private static final int WAITING = 0, RUNNING = 1, CANCELING = 2;

        private AtomicInteger state = new AtomicInteger(WAITING);

        public BulkheadTask(String name, Callable<Future<V>> callable) {
            super(name, callable);
        }

        @Override
        public void run() {
            if (state.compareAndSet(WAITING, RUNNING)) {
                try {
                    super.run();
                } finally {
                    capacitySemaphore.release();
                }
            }
        }

        public void cancel() {
            if (state.compareAndSet(WAITING, CANCELING)) {
                capacitySemaphore.release();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
