package io.smallrye.faulttolerance;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory that creates the executor services/thread pools used by SmallRye Fault Tolerance for asynchronous
 * invocations and helper threads, such as timeouts
 *
 * To create a custom one, implement this class and register it using ServiceLoade, i.e. by adding a
 * {@code META-INF/services/io.smallrye.faulttolerance.ExecutorFactory} with its fully qualified name inside.
 * 
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface ExecutorFactory {
    /**
     * create an executor service of the given size and with the given queue size
     * @param size amount of threads in the pool
     * @param queueSize the size of the waiting queue
     * @return executor service
     */
    ExecutorService createExecutorService(int size, int queueSize);

    /**
     * similar to {@link #createExecutorService(int, int)} but taking a the waiting queue as parameter.
     *
     * @param size amount of threads in the pool
     * @param queueSize the size of the waiting queue
     * @param taskQueue the waiting queue. The queue size is guaranteed to be equal to {@code queueSize}
     * @return executor service
     */
    ExecutorService createExecutorService(int size, int queueSize, BlockingQueue<Runnable> taskQueue);

    /**
     * create a scheduled executor service for handling timeouts
     * @param size the amount of threads in the pool
     * @return scheduled executor service
     */
    ScheduledExecutorService createTimeoutExecutor(int size);

    /**
     * priority of this factory.
     * If multiple factories are registered, the one with the highest priority is selected;
     * @return priority
     */
    int priority();
}
