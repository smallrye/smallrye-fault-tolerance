package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;

import io.smallrye.faulttolerance.core.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;

final class LazyDependencies implements BuilderLazyDependencies {
    private final boolean enabled;
    private final ExecutorService executor;
    private final MetricsAdapter metricsAdapter;
    private final EventLoop eventLoop;
    private final Timer timer;

    LazyDependencies(Configuration config) {
        this.enabled = config.enabled();
        this.executor = config.executor();
        this.metricsAdapter = config.metricsAdapter();
        this.eventLoop = EventLoop.get();
        this.timer = ThreadTimer.create(executor);
    }

    @Override
    public boolean ftEnabled() {
        return enabled;
    }

    @Override
    public ExecutorService asyncExecutor() {
        return executor;
    }

    @Override
    public EventLoop eventLoop() {
        return eventLoop;
    }

    @Override
    public Timer timer() {
        return timer;
    }

    @Override
    public MetricsProvider metricsProvider() {
        if (metricsAdapter instanceof NoopAdapter) {
            return ((NoopAdapter) metricsAdapter).createMetricsProvider();
        } else if (metricsAdapter instanceof MicrometerAdapter) {
            return ((MicrometerAdapter) metricsAdapter).createMetricsProvider();
        } else {
            throw new IllegalStateException("Invalid metrics adapter: " + metricsAdapter);
        }
    }

    void shutdown() throws InterruptedException {
        timer.shutdown();
    }
}
