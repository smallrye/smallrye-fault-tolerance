package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class DefaultConfiguration implements Configuration {
    private final boolean enabled = !"false".equals(System.getProperty("MP_Fault_Tolerance_NonFallback_Enabled"));
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }

    @Override
    public void onShutdown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}
