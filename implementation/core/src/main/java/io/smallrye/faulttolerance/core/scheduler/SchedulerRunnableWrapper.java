package io.smallrye.faulttolerance.core.scheduler;

import java.util.ServiceLoader;

public interface SchedulerRunnableWrapper {
    Runnable wrap(Runnable runnable);

    SchedulerRunnableWrapper INSTANCE = Loader.load();

    // ---

    class Loader {
        private static SchedulerRunnableWrapper load() {
            for (SchedulerRunnableWrapper found : ServiceLoader.load(SchedulerRunnableWrapper.class)) {
                return found;
            }
            return runnable -> runnable;
        }
    }
}
