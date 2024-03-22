package io.smallrye.faulttolerance.standalone;

import io.smallrye.faulttolerance.core.util.Preconditions;

/**
 * Integration point for standalone usage of SmallRye Fault Tolerance. Allows custom configuration
 * and shutting down the supporting infrastructure.
 * <p>
 * The {@link #configure(Configuration) configure()} method accepts an implementation of
 * the {@link Configuration} interface. It must be called before the first use of SmallRye
 * Fault Tolerance, and it may only be called once.
 * <p>
 * The {@link #shutdown()} method should be called during application shutdown, when SmallRye
 * Fault Tolerance is not supposed to be used anymore. It shuts down all internally created
 * resources (such as a timer) and then calls {@link Configuration#onShutdown()}. SmallRye
 * Fault Tolerance must not be used after {@code shutdown()} is called.
 */
public final class StandaloneFaultTolerance {
    private static Configuration configuration;
    private static LazyDependencies lazyDependencies;

    /**
     * Makes sure that the given {@code configuration} is used by SmallRye Fault Tolerance.
     * Throws an exception when configuration was already set or when SmallRye Fault Tolerance
     * has already read the configuration and reconfiguration is not possible.
     *
     * @param configuration custom {@link Configuration} implementation, must not be {@code null}
     */
    public static synchronized void configure(Configuration configuration) {
        if (StandaloneFaultTolerance.configuration != null) {
            throw new IllegalStateException("Configuration has already been finalized, cannot reconfigure");
        }
        Preconditions.checkNotNull(configuration, "Configuration must be set");
        StandaloneFaultTolerance.configuration = configuration;
    }

    /**
     * Provides access to the timer that SmallRye Fault Tolernce internally uses for scheduling
     * purposes. It provides a read-only view into what the timer is doing.
     *
     * @return read-only view into the SmallRye Fault Tolerance timer
     */
    @Deprecated(forRemoval = true)
    public static synchronized TimerAccess timerAccess() {
        // we should expose timer metrics out of the box, but this will have to do for now
        return new TimerAccessImpl(getLazyDependencies().timer());
    }

    /**
     * Asks SmallRye Fault Tolerance to shut down. If custom configuration was provided using
     * {@link #configure(Configuration) configure()}, the {@link Configuration#onShutdown()} method
     * is called as the last step.
     * <p>
     * After this method is called, no thread may use the SmallRye Fault Tolerance API.
     *
     * @throws InterruptedException when the shutdown process is interrupted
     */
    public static synchronized void shutdown() throws InterruptedException {
        InterruptedException interrupted = null;

        try {
            getLazyDependencies().shutdown();
        } catch (InterruptedException e) {
            interrupted = e;
        }

        try {
            getConfiguration().onShutdown();
        } catch (InterruptedException e) {
            interrupted = e;
        }

        if (interrupted != null) {
            throw interrupted;
        }
    }

    // ---

    static synchronized Configuration getConfiguration() {
        if (configuration == null) {
            configuration = new DefaultConfiguration();
        }
        return configuration;
    }

    static synchronized LazyDependencies getLazyDependencies() {
        if (lazyDependencies == null) {
            lazyDependencies = new LazyDependencies(getConfiguration());
        }
        return lazyDependencies;
    }
}
