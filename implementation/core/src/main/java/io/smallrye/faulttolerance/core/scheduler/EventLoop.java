package io.smallrye.faulttolerance.core.scheduler;

import static io.smallrye.faulttolerance.core.scheduler.SchedulerLogger.LOG;

import java.util.ServiceLoader;

/**
 * Discovered using {@link ServiceLoader}. At most one implementation may be present on the classpath.
 */
public interface EventLoop extends Scheduler {
    /**
     * When this method returns {@code false}, calling {@code schedule} doesn't make sense and may throw.
     */
    boolean isEventLoopThread();

    static EventLoop get() {
        for (EventLoop eventLoop : ServiceLoader.load(EventLoop.class)) {
            LOG.foundEventLoop(eventLoop);
            return eventLoop;
        }

        LOG.noEventLoop();
        return NoEventLoop.INSTANCE;
    }
}
