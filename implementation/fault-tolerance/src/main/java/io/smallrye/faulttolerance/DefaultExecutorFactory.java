package io.smallrye.faulttolerance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DefaultExecutorFactory implements ExecutorFactory {
    @Override
    public ExecutorService createExecutorService(int size, int queueSize) {
        return new ThreadPoolExecutor(size, size, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize));
    }

    @Override
    public ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue) {
        return new ThreadPoolExecutor(size, size, 0, TimeUnit.MILLISECONDS, taskQueue);
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
