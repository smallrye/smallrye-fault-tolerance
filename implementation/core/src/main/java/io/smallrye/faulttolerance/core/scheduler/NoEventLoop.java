package io.smallrye.faulttolerance.core.scheduler;

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
    public SchedulerTask schedule(long delayInMillis, Runnable runnable) {
        throw new UnsupportedOperationException();
    }
}
