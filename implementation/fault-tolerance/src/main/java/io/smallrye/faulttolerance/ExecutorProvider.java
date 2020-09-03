package io.smallrye.faulttolerance;

import java.util.OptionalInt;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.core.timer.Timer;

/**
 * Provider of thread pools for bulkheads and asynchronous invocations
 */
@Singleton
public class ExecutorProvider {
    private static final int DEFAULT_MAIN_THREAD_P0OL_SIZE = 100;

    private final ExecutorService mainExecutor;

    private final Timer timer;

    @Inject
    public ExecutorProvider(
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolSize") OptionalInt mainThreadPoolSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.globalThreadPoolSize") OptionalInt globalThreadPoolSize) {

        int mainExecutorSize = mainThreadPoolSize.orElse(globalThreadPoolSize.orElse(DEFAULT_MAIN_THREAD_P0OL_SIZE));

        if (mainExecutorSize < 5) {
            throw new IllegalArgumentException("The main thread pool size must be >= 5.");
        }

        ExecutorFactory executorFactory = executorFactory();
        this.mainExecutor = executorFactory.createCoreExecutor(mainExecutorSize);
        this.timer = new Timer(mainExecutor);
    }

    @PreDestroy
    public void tearDown() {
        try {
            timer.shutdown();
        } catch (InterruptedException e) {
            // no need to do anything, we're shutting down anyway
            // just set the interruption flag to be a good citizen
            Thread.currentThread().interrupt();
        }

        mainExecutor.shutdownNow();
        try {
            mainExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // no need to do anything, we're shutting down anyway
            // just set the interruption flag to be a good citizen
            Thread.currentThread().interrupt();
        }
    }

    public ExecutorService getMainExecutor() {
        return mainExecutor;
    }

    public Timer getTimer() {
        return timer;
    }

    private static ExecutorFactory executorFactory() {
        ExecutorFactory maxPriority = new DefaultExecutorFactory();

        for (ExecutorFactory factory : ServiceLoader.load(ExecutorFactory.class)) {
            if (factory.priority() > maxPriority.priority()) {
                maxPriority = factory;
            }
        }

        return maxPriority;
    }
}
