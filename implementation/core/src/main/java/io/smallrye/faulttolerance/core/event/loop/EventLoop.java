package io.smallrye.faulttolerance.core.event.loop;

import static io.smallrye.faulttolerance.core.event.loop.EventLoopLogger.LOG;

import java.util.ServiceLoader;
import java.util.concurrent.Executor;

/**
 * Discovered using {@link ServiceLoader}. At most one implementation may be present on the classpath.
 */
public interface EventLoop {
    /**
     * Returns an {@link Executor} that runs tasks on the current thread's event loop.
     * If the current thread is not an event loop thread, returns {@code null}.
     * <p>
     * Pay attention to when you call this method. If you want to <em>later</em> use an executor
     * for current thread's event loop, possibly even from a different thread, call this method
     * early and remember the result.
     */
    Executor executor();

    static EventLoop get() {
        for (EventLoop eventLoop : ServiceLoader.load(EventLoop.class)) {
            LOG.foundEventLoop(eventLoop);
            return eventLoop;
        }

        LOG.noEventLoop();
        return NoEventLoop.INSTANCE;
    }
}
