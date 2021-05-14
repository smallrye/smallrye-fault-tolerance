package io.smallrye.faulttolerance.core.scheduler;

import io.smallrye.faulttolerance.core.util.Preconditions;

/**
 * Schedules tasks on an {@link EventLoop} if the caller is on an event loop thread, otherwise uses a {@link Timer}.
 */
public final class MainScheduler implements Scheduler {
    private final EventLoop eventLoop;
    private final Timer timer;

    public MainScheduler(EventLoop eventLoop, Timer timer) {
        this.eventLoop = Preconditions.checkNotNull(eventLoop, "Event loop must be set");
        this.timer = Preconditions.checkNotNull(timer, "Timer must be set");
    }

    @Override
    public SchedulerTask schedule(long delayInMillis, Runnable runnable) {
        if (eventLoop.isEventLoopThread()) {
            return eventLoop.scheduler().schedule(delayInMillis, runnable);
        } else {
            return timer.schedule(delayInMillis, runnable);
        }
    }
}
