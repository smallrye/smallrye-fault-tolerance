//package com.github.ladicek.oaken_ocean.core.bulkhead;
//
//import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
//import com.github.ladicek.oaken_ocean.core.FutureOrFailure;
//import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
//
//import java.util.concurrent.Callable;
//import java.util.concurrent.Future;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.RejectedExecutionException;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
///**
// * This class is a one big TODO :)
// */
//public class FutureBulkhead<V> extends Bulkhead<Future<V>> { // mstodo name
//    private final ThreadPoolExecutor executor;
//    private final LinkedBlockingQueue<Runnable> workQueue;
//
//    public FutureBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, int size, int queueSize,
//                          MetricsRecorder metricsRecorder) {
//        super(delegate, description, size, metricsRecorder);
//
//        workQueue = new LinkedBlockingQueue<>(queueSize);
//        executor = new ThreadPoolExecutor(size, size,
//              0L, TimeUnit.MILLISECONDS,
//              workQueue);
//    }
//
//    @Override
//    public Future<V> apply(Callable<Future<V>> target) throws Exception {
//        System.out.print("enqueuing...\t");
//        try {
//            FutureOrFailure<V> result = new FutureOrFailure<>();
//            BulkheadTask bulkheadTask = new BulkheadTask(System.nanoTime(), target, result);
//            // mstodo get rid of passing the result in the bulkhead task
//            executor.execute(bulkheadTask);
//            recorder.bulkheadQueueEntered();
//
//            System.out.println("[ENQUEUED]");
//            System.out.println("waiting for future initialization");
//            try {
//                result.waitForFutureInitialization(); // mstodo: sort of kills the idea of separate thread, this thread will wait for the bulkhead thread...
//            } catch (InterruptedException e) {
//                System.out.println("interrupting bulkhead execution");
//                workQueue.remove(bulkheadTask);
//                bulkheadTask.interrupt();
//                // mstodo what to return?
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw e;
//            }
//            System.out.println("finished waiting, thread interrupted: " + Thread.interrupted());
//
//            if (result.isCancelled()) {
//                bulkheadTask.interrupt(); // mstodo pass through the mayInterrupt... ?
//                workQueue.remove(bulkheadTask);
//            }
//
//            System.out.println("done");
//            return result;
//        } catch (RejectedExecutionException queueFullException) {
//            System.out.println("[REJECTED]");
//            recorder.bulkheadRejected();
//            throw new BulkheadException(); // mstodo
//        }
//    }
//
//    private class BulkheadTask implements Runnable {
//        private final long timeEnqueued;
//        private final Callable<Future<V>> task;
//        private final FutureOrFailure<V> result;
//        private Thread myThread;
//
//        private BulkheadTask(long timeEnqueued, Callable<Future<V>> task, FutureOrFailure<V> result) {
//            this.timeEnqueued = timeEnqueued;
//            this.task = task;
//            this.result = result;
//        }
//
//        @Override
//        public void run() {
//            myThread = Thread.currentThread();
//            long startTime = System.nanoTime();
//            recorder.bulkheadEntered(startTime - timeEnqueued);
//            try {
//                result.setDelegate(task.call());
//            } catch (Exception e) {
//                result.setFailure(e);
//            } finally {
//                myThread = null;
//                recorder.bulkheadLeft(System.nanoTime() - startTime);
//            }
//        }
//
//        public void interrupt() {
//            if (myThread != null) {
//                myThread.interrupt();
//            }
//        }
//    }
//}
