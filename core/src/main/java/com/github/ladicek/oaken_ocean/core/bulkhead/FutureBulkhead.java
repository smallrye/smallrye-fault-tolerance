package com.github.ladicek.oaken_ocean.core.bulkhead;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is a one big TODO :)
 */
public class FutureBulkhead<V> extends Bulkhead<Future<V>> { // mstodo name
    private final MetricsRecorder recorder;

    private final ThreadPoolExecutor executor;

    public FutureBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, int size, int queueSize,
                          MetricsRecorder metricsRecorder) {
        super(delegate, description, size, metricsRecorder);
        this.recorder = metricsRecorder == null ? MetricsRecorder.NOOP : metricsRecorder;

        executor = new ThreadPoolExecutor(size, size,
              0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<>(queueSize));
    }

    @Override
    public Future<V> apply(Callable<Future<V>> target) throws Exception {
        try {
            FutureOrFailure<V> result = new FutureOrFailure<>();
            executor.execute(new BulkheadTask(System.nanoTime(), target, result));
            recorder.bulkheadQueueEntered();

            result.waitForFutureInitialization(); // mstodo: sort of kills the idea of separate thread, this thread will wait for the bulkhead thread...
            return result;
        } catch (RejectedExecutionException queueFullException) {
            recorder.bulkheadRejected();
            throw new BulkheadException(); // mstodo
        }
    }

    private class BulkheadTask implements Runnable {
        private final long timeEnqueued;
        private final Callable<Future<V>> task;
        private final FutureOrFailure<V> result;

        private BulkheadTask(long timeEnqueued, Callable<Future<V>> task, FutureOrFailure<V> result) {
            this.timeEnqueued = timeEnqueued;
            this.task = task;
            this.result = result;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();
            recorder.bulkheadEntered(startTime - timeEnqueued);
            try {
                result.setDelegate(task.call());
            } catch (Exception e) {
                result.setFailure(e);
            } finally {
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        }
    }
}
