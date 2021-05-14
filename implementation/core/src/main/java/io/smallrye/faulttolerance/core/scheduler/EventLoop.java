package io.smallrye.faulttolerance.core.scheduler;

import static io.smallrye.faulttolerance.core.scheduler.SchedulerLogger.LOG;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;

/**
 * Discovered using {@link ServiceLoader}. At most one implementation may be present on the classpath.
 */
public interface EventLoop {
    /**
     * Returns whether current thread is an event loop thread.
     * <p>
     * When this method returns {@code false}, calling {@link #executor()} or {@link #scheduler()}
     * doesn't make sense and throws {@link UnsupportedOperationException}.
     */
    boolean isEventLoopThread();

    /**
     * Returns an {@link Executor} that runs tasks on the current thread's event loop.
     * <p>
     * Pay attention to when you call this method. If you want to <em>later</em> use an executor
     * for current thread's event loop, possibly even from a different thread, call this method
     * early and remember the result.
     */
    Executor executor();

    /**
     * Returns a {@link Scheduler} that schedules tasks on the current thread's event loop.
     * <p>
     * Pay attention to when you call this method. If you want to <em>immediately</em> use a scheduler
     * for current thread's event loop, call this method late, immediately before calling {@code schedule},
     * and avoid remembering the result.
     */
    Scheduler scheduler();

    static EventLoop get() {
        for (EventLoop eventLoop : ServiceLoader.load(EventLoop.class)) {
            LOG.foundEventLoop(eventLoop);
            return eventLoop;
        }

        LOG.noEventLoop();
        return NoEventLoop.INSTANCE;
    }
}
