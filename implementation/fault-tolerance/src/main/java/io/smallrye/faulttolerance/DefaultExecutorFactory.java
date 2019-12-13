package io.smallrye.faulttolerance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DefaultExecutorFactory implements ExecutorFactory {

    private static final int KEEP_ALIVE_TIME = 10 * 60;

    @Override
    public ExecutorService createExecutorService(int size, int queueSize) {
        BlockingQueue<Runnable> queue;
        if (queueSize == 0) {
            queue = new SynchronousQueue<>();
        } else {
            queue = new LinkedBlockingQueue<>(queueSize);
        }
        return new ThreadPoolExecutor(1, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, queue);
    }

    @Override
    public ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue) {
        return new ThreadPoolExecutor(size, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, taskQueue);
    }

    @Override
    public ScheduledExecutorService createTimeoutExecutor(int size) {
        return Executors.newScheduledThreadPool(size);
    }

    @Override
    public int priority() {
        return 0;
    }
}
