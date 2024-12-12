package io.smallrye.faulttolerance.core.event.loop;

import java.util.concurrent.Executor;

final class NoEventLoop implements EventLoop {
    static final NoEventLoop INSTANCE = new NoEventLoop();

    private NoEventLoop() {
        // avoid instantiation
    }

    @Override
    public Executor executor() {
        return null;
    }
}
