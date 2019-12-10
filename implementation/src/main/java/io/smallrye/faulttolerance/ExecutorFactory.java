package io.smallrye.faulttolerance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * mstodo docummentation
 * 
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface ExecutorFactory {
    ExecutorService createExecutorService(int size, int queueSize);

    ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue);

    ScheduledExecutorService createTimeoutExecutor(int size);

    int priority();
}
