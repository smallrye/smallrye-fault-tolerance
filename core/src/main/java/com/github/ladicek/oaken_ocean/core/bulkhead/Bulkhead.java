package com.github.ladicek.oaken_ocean.core.bulkhead;

import com.github.ladicek.oaken_ocean.core.Cancelator;
import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureOrFailure;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class is a one big TODO :)
 */
public class Bulkhead<V> implements FaultToleranceStrategy<V> {
    final FaultToleranceStrategy<V> delegate;
    private final String description;

    private final Semaphore bulkheadSemaphore;
    final MetricsRecorder recorder;

    private final ThreadPoolExecutor executor;
    private final LinkedBlockingQueue<Runnable> workQueue;


    public Bulkhead(FaultToleranceStrategy<V> delegate, String description, int size, int queueSize,
                          MetricsRecorder metricsRecorder) {
        this.delegate = delegate;
        this.description = description;
        this.bulkheadSemaphore = new Semaphore(size);
        this.recorder = metricsRecorder == null ? MetricsRecorder.NOOP : metricsRecorder;
        workQueue = new LinkedBlockingQueue<>(queueSize);
        executor = new ThreadPoolExecutor(size, size,
              0L, TimeUnit.MILLISECONDS,
              workQueue);
    }

    @Override
    public V apply(Callable<V> target) throws Exception {
        if (bulkheadSemaphore.tryAcquire()) {
            recorder.bulkheadEntered();
            long startTime = System.nanoTime();
            try {
                return delegate.apply(target);
            } finally {
                bulkheadSemaphore.release();
                recorder.bulkheadLeft(System.nanoTime() - startTime);
            }
        } else {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }

    @Override
    public V asyncFutureApply(Callable<V> target, Cancelator cancelator) throws Exception {
        try {
            FutureOrFailure result = new FutureOrFailure<>();
            BulkheadTask bulkheadTask = new BulkheadTask(System.nanoTime(), target, result);
            // mstodo get rid of passing the result in the bulkhead task
            executor.execute(bulkheadTask);
            cancelator.addCancelAction(() -> workQueue.remove(bulkheadTask));
            recorder.bulkheadQueueEntered();

            try {
                result.waitForFutureInitialization(); // mstodo: sort of kills the idea of separate thread, this thread will wait for the bulkhead thread...
            } catch (InterruptedException e) {
                workQueue.remove(bulkheadTask);
                bulkheadTask.interrupt();
                // mstodo what to return?
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            return (V)result;
        } catch (RejectedExecutionException queueFullException) {
            recorder.bulkheadRejected();
            throw bulkheadRejected();
        }
    }

    private BulkheadException bulkheadRejected() {
        return new BulkheadException(description + " rejected from bulkhead");
    }

    private class BulkheadTask<W> implements Runnable {
        private final long timeEnqueued;
        private final Callable<Future<W>> task;
        private final FutureOrFailure<W> result;
        private Thread myThread;

        private BulkheadTask(long timeEnqueued, Callable<Future<W>> task, FutureOrFailure<W> result) {
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
                result.setDelegate(task.call());
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

    public interface MetricsRecorder {
        void bulkheadQueueEntered();
        void bulkheadQueueLeft(long timeInQueue);
        void bulkheadEntered();
        void bulkheadRejected();
        void bulkheadLeft(long processingTime);

        MetricsRecorder NOOP = new MetricsRecorder() {
            @Override
            public void bulkheadQueueEntered() {
            }

            @Override
            public void bulkheadQueueLeft(long timeInQueue) {
            }

            @Override
            public void bulkheadEntered() {
            }

            @Override
            public void bulkheadRejected() {
            }

            @Override
            public void bulkheadLeft(long processingTime) {
            }
        };
    }
}
