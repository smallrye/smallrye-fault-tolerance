package com.github.ladicek.oaken_ocean.core.bulkhead;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.FutureOrFailure;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureBulkhead<V> extends BulkheadBase<Future<V>, FutureInvocationContext<V>> {

    private final ThreadPoolExecutor executor;
    private final LinkedBlockingQueue<Runnable> workQueue;

    public FutureBulkhead(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate, String description,
            int size, int queueSize,
            MetricsRecorder recorder) {
        super(description, delegate, recorder);
        workQueue = new LinkedBlockingQueue<>(queueSize);
        executor = new ThreadPoolExecutor(size, size,
                0L, TimeUnit.MILLISECONDS,
                workQueue);
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        try {
            FutureOrFailure<V> result = new FutureOrFailure<>();
            BulkheadTask bulkheadTask = new BulkheadTask(System.nanoTime(), target, result);
            // mstodo get rid of passing the result in the bulkhead task
            executor.execute(bulkheadTask);
            if (target.getCancellator() != null) {
                target.getCancellator().addCancelAction(ignored -> workQueue.remove(bulkheadTask));
            }
            recorder.bulkheadQueueEntered();

            try {
                result.waitForFutureInitialization(); // mstodo: sort of kills the idea of separate thread, this thread will wait for the bulkhead thread...
            } catch (InterruptedException e) {
                workQueue.remove(bulkheadTask);
                bulkheadTask.interrupt();
                // mstodo what to return?
            }
            return result;
        } catch (RejectedExecutionException queueFullException) {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }

    int getQueueSize() {
        return workQueue.size();
    }

    private class BulkheadTask implements Runnable {
        private final long timeEnqueued;
        private final FutureInvocationContext<V> task;
        private final FutureOrFailure<V> result;
        private Thread myThread;

        private BulkheadTask(long timeEnqueued, FutureInvocationContext<V> task, FutureOrFailure<V> result) {
            this.timeEnqueued = timeEnqueued;
            this.task = task;
            this.result = result;
        }

        @Override
        public void run() {
            myThread = Thread.currentThread();
            long startTime = System.nanoTime();
            recorder.bulkheadQueueLeft(startTime - timeEnqueued);
            recorder.bulkheadEntered();
            try {
                result.setDelegate(delegate.apply(task));
            } catch (Exception e) {
                result.setFailure(e);
            } finally {
                myThread = null;
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        }

        public void interrupt() {
            if (myThread != null) {
                myThread.interrupt();
            }
        }
    }
}
