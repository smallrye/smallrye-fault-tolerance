package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;

public interface Configuration {
    /**
     * Returns whether fault tolerance strategies should be enabled.
     * If they are disabled, only <em>fallback</em> and <em>thread offload</em> will be used.
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Returns the executor for thread offloads.
     */
    ExecutorService executor();

    /**
     * Returns the adapter to be used for emitting metrics.
     * Use {@link NoopAdapter} if metrics should be disabled.
     * Use {@link MicrometerAdapter} if metrics should be emitted to given Micrometer registry.
     */
    default MetricsAdapter metricsAdapter() {
        return NoopAdapter.INSTANCE;
    }

    /**
     * Callback executed at the very end of {@link StandaloneFaultTolerance#shutdown()},
     * when all internal resources have been shut down.
     */
    default void onShutdown() throws InterruptedException {
    }
}
