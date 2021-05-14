package io.smallrye.faulttolerance.core.scheduler;

import java.util.concurrent.Executor;

final class NoEventLoop implements EventLoop {
    static final NoEventLoop INSTANCE = new NoEventLoop();

    private NoEventLoop() {
        // avoid instantiation
    }

    @Override
    public boolean isEventLoopThread() {
        return false;
    }

    @Override
    public Executor executor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Scheduler scheduler() {
        throw new UnsupportedOperationException();
    }
}
