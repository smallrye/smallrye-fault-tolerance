package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory that creates the executor services/thread pools used by SmallRye Fault Tolerance for asynchronous
 * invocations and thread pool style bulkheads.
 *
 * To create a custom one, implement this class and register it using ServiceLoade, i.e. by adding a
 * {@code META-INF/services/io.smallrye.faulttolerance.ExecutorFactory} with its fully qualified name inside.
 * Pay attention to the {@code priority} method, too.
 * 
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface ExecutorFactory {
    /**
     * Create an executor service of the given size
     * This executor may have a small core pool size and, if possible, should not be queued
     * 
     * @param size amount of threads in the pool
     * @return executor service
     */
    ExecutorService createCoreExecutor(int size);

    /**
     * similar to {@link #createCoreExecutor(int)} but creating an executor with unlimited (or large) queue size
     * with defined core pool size
     *
     * @param coreSize amount of threads in the pool's core
     * @param size amount of threads in the pool
     * @return executor service
     */
    ExecutorService createExecutor(int coreSize, int size);

    /**
     * @deprecated this is no longer used
     */
    @Deprecated
    default ScheduledExecutorService createTimeoutExecutor(int size) {
        throw new UnsupportedOperationException();
    }

    /**
     * Priority of this factory.
     * If multiple factories are registered, the one with the highest priority is used.
     * If multiple factories have the same priority, which one is used is undefined.
     */
    int priority();
}
