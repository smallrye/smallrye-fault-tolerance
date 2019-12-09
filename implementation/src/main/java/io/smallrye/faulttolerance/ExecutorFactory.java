package io.smallrye.faulttolerance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface ExecutorFactory {
    ExecutorService getGlobalExecutorService(int size, int queueSize);

    ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue);

    int priority();
}
