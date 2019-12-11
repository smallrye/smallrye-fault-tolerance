package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.NamedFutureTask;

public class ThreadPoolBulkhead<V> extends BulkheadBase<Future<V>> {
    private final ExecutorService executor;
    private final BlockingQueue<Runnable> queue;

    public ThreadPoolBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, ExecutorService executor,
            BlockingQueue<Runnable> queue, MetricsRecorder recorder) {
        super(description, delegate, recorder);
        this.executor = executor;
        this.queue = queue;
    }

    @Override
    public Future<V> apply(InvocationContext<Future<V>> ctx) throws Exception {
        try {
            long timeEnqueued = System.nanoTime();
            FutureTask<Future<V>> task = new NamedFutureTask<>("ThreadPoolBulkhead", () -> {
                long startTime = System.nanoTime();
                recorder.bulkheadQueueLeft(startTime - timeEnqueued);
                recorder.bulkheadEntered();
                try {
                    return delegate.apply(ctx);
                } finally {
                    recorder.bulkheadLeft(System.nanoTime() - startTime);
                }
            });
            ctx.registerEventHandler(InvocationContext.Event.CANCEL, () -> queue.remove(task));
            executor.execute(task);
            recorder.bulkheadQueueEntered();

            try {
                return task.get();
            } catch (InterruptedException e) {
                queue.remove(task);
                throw e; // TODO
            } catch (ExecutionException e) {
                throw sneakyThrow(e.getCause());
            }
        } catch (RejectedExecutionException e) {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }

    // only for testing
    int getQueueSize() {
        return queue.size();
    }

    // TODO
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> Exception sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
