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

    private volatile MetricsProvider metricsProvider;

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
        MetricsProvider metricsProvider = this.metricsProvider;
        if (metricsProvider == null) {
            synchronized (this) {
                metricsProvider = this.metricsProvider;
                if (metricsProvider == null) {
                    if (metricsAdapter instanceof NoopAdapter) {
                        metricsProvider = ((NoopAdapter) metricsAdapter).createMetricsProvider();
                    } else if (metricsAdapter instanceof MicrometerAdapter) {
                        metricsProvider = ((MicrometerAdapter) metricsAdapter).createMetricsProvider();
                    } else {
                        throw new IllegalStateException("Invalid metrics adapter: " + metricsAdapter);
                    }
                    this.metricsProvider = metricsProvider;
                }
            }
        }

        return metricsProvider;
    }

    void shutdown() throws InterruptedException {
        timer.shutdown();
    }
}
