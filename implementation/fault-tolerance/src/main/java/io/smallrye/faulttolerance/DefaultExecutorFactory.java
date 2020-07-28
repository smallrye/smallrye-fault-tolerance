package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DefaultExecutorFactory implements ExecutorFactory {

    private static final int KEEP_ALIVE_TIME = 10 * 60;

    @Override
    public ExecutorService createCoreExecutor(int size) {
        return new ThreadPoolExecutor(1, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory());
    }

    @Override
    public ExecutorService createExecutor(int coreSize, int size) {
        return new ThreadPoolExecutor(coreSize, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                threadFactory());
    }

    /**
     * Can be overridden in a subclass to provide a different {@link ThreadFactory}.
     * Useful e.g. in a Jakarta EE container, where the threads must be managed.
     */
    protected ThreadFactory threadFactory() {
        return Executors.defaultThreadFactory();
    }

    @Override
    public int priority() {
        return 0;
    }
}
