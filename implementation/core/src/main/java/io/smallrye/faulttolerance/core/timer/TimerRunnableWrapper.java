package io.smallrye.faulttolerance.core.timer;

import java.util.ServiceLoader;

public interface TimerRunnableWrapper {
    Runnable wrap(Runnable runnable);

    static TimerRunnableWrapper load() {
        for (TimerRunnableWrapper found : ServiceLoader.load(TimerRunnableWrapper.class)) {
            return found;
        }
        return runnable -> runnable;
    }
}
