package io.smallrye.faulttolerance.core.retry;

import java.util.function.Supplier;

/**
 * Performs a delay asynchronously (i.e., schedules task execution, potentially on another thread).
 */
public interface AsyncDelay {
    void after(Runnable task);

    Supplier<AsyncDelay> NONE = () -> Runnable::run;
}
