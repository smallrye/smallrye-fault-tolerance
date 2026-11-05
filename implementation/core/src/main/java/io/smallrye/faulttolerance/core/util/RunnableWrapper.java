package io.smallrye.faulttolerance.core.util;

import java.util.ServiceLoader;

/**
 * Used by {@link io.smallrye.faulttolerance.core.timer.Timer Timer},
 * {@link io.smallrye.faulttolerance.core.event.loop.EventLoop EventLoop}
 * and {@link io.smallrye.faulttolerance.core.bulkhead.Bulkhead Bulkhead}.
 * <p>
 * Implementations are expected to only decorate the runnable with context
 * setup/teardown, and keep it running it on the thread that called
 * {@code Runnable.run()}. They must not move the execution to another thread.
 */
public interface RunnableWrapper {
    Runnable wrap(Runnable runnable);

    RunnableWrapper INSTANCE = Loader.load();

    // ---

    class Loader {
        private static RunnableWrapper load() {
            for (RunnableWrapper found : ServiceLoader.load(RunnableWrapper.class)) {
                return found;
            }
            return runnable -> runnable;
        }
    }
}
