package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.FutureOrFailure;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class FutureBulkhead<V> extends BulkheadBase<Future<V>, FutureInvocationContext<V>> {

    private final ExecutorService executor;
    private final BlockingQueue<Runnable> workQueue;

    public FutureBulkhead(FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> delegate,
            String description,
            ExecutorService executor,
            BlockingQueue<Runnable> workQueue,
            MetricsRecorder recorder) {
        super(description, delegate, recorder);
        this.workQueue = workQueue;
        this.executor = executor;
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        try {
            FutureOrFailure<V> result = new FutureOrFailure<>();
            BulkheadTask bulkheadTask = new BulkheadTask(System.nanoTime(), target, result);
            executor.execute(bulkheadTask);
            if (target.getCancellator() != null) {
                target.getCancellator().addCancelAction(ignored -> workQueue.remove(bulkheadTask));
            }
            recorder.bulkheadQueueEntered();

            try {
                // todo: sort of kills the idea of separate thread, this thread will wait for the bulkhead thread...
                result.waitForFutureInitialization();
            } catch (InterruptedException e) {
                workQueue.remove(bulkheadTask);
                bulkheadTask.interrupt();
                result.setFailure(e);
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
