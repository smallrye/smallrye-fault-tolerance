package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;

import io.smallrye.faulttolerance.core.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;

final class LazyDependencies implements BuilderLazyDependencies {
    private final boolean enabled;
    private final ExecutorService executor;
    private final EventLoop eventLoop;
    private final Timer timer;

    LazyDependencies(Configuration config) {
        this.enabled = config.enabled();
        this.executor = config.executor();
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

    void shutdown() throws InterruptedException {
        timer.shutdown();
    }
}
