package com.github.ladicek.oaken_ocean.core.bulkhead;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class CompletionStageBulkhead<V> extends BulkheadBase<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> {
    private static final Logger logger = Logger.getLogger(CompletionStageBulkhead.class);

    private final ThreadPoolExecutor executor;
    private final LinkedBlockingQueue<CompletionStageBulkheadTask> workQueue;
    private final Semaphore workSemaphore;

    /* mstodo test that interruption does not mess up the pool */
    public CompletionStageBulkhead(String description,
            FaultToleranceStrategy<CompletionStage<V>, SimpleInvocationContext<CompletionStage<V>>> delegate,
            int size, int queueSize,
            MetricsRecorder recorder) {
        super(description, delegate, recorder);
        workQueue = new LinkedBlockingQueue<>(queueSize);
        workSemaphore = new Semaphore(size);
        // mstodo do we need daemons here ?
        // mstodo then we can ignore interruptions
        // mstodo and this is what we prob'ly need
        executor = new ThreadPoolExecutor(size, size,
                0L, TimeUnit.MILLISECONDS,
                // we don't want anything added here, it should be all handled manually in the workQueue
                // LinkedBlockingQueue doesn't support zero size
                new LinkedBlockingQueue<>(1));
        for (int i = 0; i < size; i++) {
            executor.submit(this::run);
        }
    }

    @Override
    public CompletionStage<V> apply(SimpleInvocationContext<CompletionStage<V>> context) throws Exception {
        CompletionStageBulkheadTask task = new CompletionStageBulkheadTask(System.nanoTime(), context);
        try {
            workQueue.add(task);
            recorder.bulkheadQueueEntered();
        } catch (IllegalStateException queueFullException) {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
        return task.result;
    }

    public void run() {
        try {
            workSemaphore.acquire();
        } catch (InterruptedException e) {
            logger.error("Bulkhead worker interrupted, exiting", e);
        }
        try {
            CompletionStageBulkheadTask task = workQueue.poll();
            task.execute();
        } catch (Exception any) {
            logger.error("Error processing bulkhead task", any);
        } finally {
            workSemaphore.release();
        }
    }

    private class CompletionStageBulkheadTask {
        private final long timeEnqueued;
        private final CompletableFuture<V> result = new CompletableFuture<>();
        private final SimpleInvocationContext<CompletionStage<V>> context;

        private CompletionStageBulkheadTask(long timeEnqueued, SimpleInvocationContext<CompletionStage<V>> context) {
            this.timeEnqueued = timeEnqueued;
            this.context = context;
        }

        public CompletionStage<V> execute() {
            CompletionStage<V> rawResult;
            long startTime = System.nanoTime();
            recorder.bulkheadQueueLeft(startTime - timeEnqueued);
            recorder.bulkheadEntered();
            try {
                rawResult = delegate.apply(context);
                rawResult.whenComplete((value, error) -> {
                    recorder.bulkheadLeft(System.nanoTime() - startTime);
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(value);
                    }
                });
            } catch (Exception e) {
                recorder.bulkheadLeft(System.nanoTime() - startTime);
                result.completeExceptionally(e);
            }
            return result;
        }
    }
}
