package io.smallrye.faulttolerance.core.util;

import java.util.ServiceLoader;

/**
 * Used by {@link io.smallrye.faulttolerance.core.timer.Timer Timer}
 * and {@link io.smallrye.faulttolerance.core.event.loop.EventLoop EventLoop}.
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
