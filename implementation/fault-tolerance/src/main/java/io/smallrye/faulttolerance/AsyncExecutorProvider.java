package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;

/**
 * Integrators should provide a CDI bean which implements {@link AsyncExecutorProvider}. The bean should be
 * {@code @Dependent}, must be marked as alternative and selected globally for the application.
 */
public interface AsyncExecutorProvider {
    /**
     * Provides the thread pool for executing {@code @Asynchronous} methods and other asynchronous tasks.
     * Integrator is responsible for the thread pool's lifecycle.
     */
    ExecutorService get();
}
