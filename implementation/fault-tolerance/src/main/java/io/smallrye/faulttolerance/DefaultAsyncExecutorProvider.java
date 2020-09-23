package io.smallrye.faulttolerance;

import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Default implementation of {@link AsyncExecutorProvider}.
 * Manages its own thread pool.
 * <p>
 * If integrators don't want to manage the fault tolerance thread pool,
 * yet still want to customize the thread factory, they can provide
 * an {@code @Alternative} bean which inherits from this class.
 */
@Dependent
public class DefaultAsyncExecutorProvider implements AsyncExecutorProvider {
    private final ExecutorService executor;

    @Inject
    public DefaultAsyncExecutorProvider(
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolSize") OptionalInt mainThreadPoolSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.globalThreadPoolSize") OptionalInt globalThreadPoolSize) {

        int maxSize = mainThreadPoolSize.orElse(globalThreadPoolSize.orElse(100));

        if (maxSize < 5) {
            throw new IllegalArgumentException("The main thread pool size must be >= 5.");
        }

        this.executor = new ThreadPoolExecutor(1, maxSize, 1, TimeUnit.MINUTES, new SynchronousQueue<>(), threadFactory());
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
