package io.smallrye.faulttolerance;

import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Default implementation of {@link AsyncExecutorProvider}.
 * Manages its own thread pool.
 * <p>
 * If integrators don't want to manage the fault tolerance thread pool,
 * yet still want to customize the thread factory, they can provide
 * an {@code @Alternative} bean which inherits from this class.
 */
@Singleton
public class DefaultAsyncExecutorProvider implements AsyncExecutorProvider {
    private final ExecutorService executor;

    @Inject
    public DefaultAsyncExecutorProvider(
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolSize") OptionalInt mainThreadPoolSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolQueueSize") OptionalInt mainThreadPoolQueueSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.globalThreadPoolSize") OptionalInt globalThreadPoolSize) {

        int maxSize = mainThreadPoolSize.orElse(globalThreadPoolSize.orElse(100));
        int queueSize = mainThreadPoolQueueSize.orElse(-1);

        if (maxSize < 5) {
            throw new IllegalArgumentException("The main thread pool size must be >= 5.");
        }

        if (queueSize < -1) {
            throw new IllegalArgumentException("The main thread pool queue size must be -1, 0, or > 1");
        }

        BlockingQueue<Runnable> queue;
        if (queueSize > 1) {
            queue = new LinkedBlockingQueue<>(queueSize);
        } else if (queueSize == 0) {
            queue = new SynchronousQueue<>();
        } else {
            queue = new LinkedBlockingQueue<>();
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(maxSize, maxSize, 1, TimeUnit.MINUTES, queue, threadFactory());
        executor.allowCoreThreadTimeOut(true);
        this.executor = executor;
    }

    @Override
    public ExecutorService get() {
        return executor;
    }

    /**
     * Can be overridden in a subclass to provide a different {@link ThreadFactory}.
     * Useful e.g. in a Jakarta EE container, where the threads must be managed.
     */
    protected ThreadFactory threadFactory() {
        return Executors.defaultThreadFactory();
    }
}
