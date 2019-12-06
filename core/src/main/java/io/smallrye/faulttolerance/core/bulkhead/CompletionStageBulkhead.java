package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.SimpleInvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CompletionStageBulkhead<V> extends BulkheadBase<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> {
    private static final Logger logger = Logger.getLogger(CompletionStageBulkhead.class);

    private final ThreadPoolExecutor executor;
    private final int queueSize;
    private final int size;
    private final Semaphore workSemaphore;
    private final Semaphore capacitySemaphore;

    public CompletionStageBulkhead(
            FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
            String description,
            int size, int queueSize,
            MetricsRecorder recorder) {
        super(description, delegate, recorder);
        workSemaphore = new Semaphore(size);
        capacitySemaphore = new Semaphore(size + queueSize);
        this.queueSize = queueSize;
        this.size = size;
        executor = new ThreadPoolExecutor(size, size,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize));
    }

    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> context) {
        if (capacitySemaphore.tryAcquire()) {
            CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(System.nanoTime(), context);
            executor.execute(task);
            recorder.bulkheadQueueEntered();
            return task.result;
        } else {
            recorder.bulkheadRejected();
            CompletableFuture<V> result = new CompletableFuture<>();
            result.completeExceptionally(bulkheadRejected());
            return result;
        }
    }

    public int getQueueSize() {
        return Math.min(queueSize, size + queueSize - capacitySemaphore.availablePermits());
    }

    private class CompletionStageBulkheadTask implements Runnable {
        private final long timeEnqueued;
        private final CompletableFuture<V> result = new CompletableFuture<>();
        private final SimpleInvocationContext<CompletionStage<V>> context;

        private CompletionStageBulkheadTask(long timeEnqueued,
                SimpleInvocationContext<CompletionStage<V>> context) {
            this.timeEnqueued = timeEnqueued;
            this.context = context;
        }

        public void run() {
            try {
                workSemaphore.acquire();
            } catch (InterruptedException e) {
                logger.error("Bulkhead worker interrupted, exiting", e);
                result.completeExceptionally(e);
                return;
            }

            CompletionStage<V> rawResult;
            long startTime = System.nanoTime();
            recorder.bulkheadQueueLeft(startTime - timeEnqueued);
            recorder.bulkheadEntered();
            try {
                rawResult = delegate.apply(context);
                rawResult.whenComplete((value, error) -> {
                    releaseSemaphores();
                    recorder.bulkheadLeft(System.nanoTime() - startTime);
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(value);
                    }
                });
            } catch (Exception e) {
                releaseSemaphores();
                recorder.bulkheadLeft(System.nanoTime() - startTime);
                result.completeExceptionally(e);
            }
        }
    }

    private void releaseSemaphores() {
        workSemaphore.release();
        capacitySemaphore.release();
    }
}
