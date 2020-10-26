package io.smallrye.faulttolerance.core.retry;

import java.util.function.Supplier;

/**
 * Performs a delay synchronously (on the calling thread).
 */
public interface SyncDelay {
    void sleep() throws InterruptedException;

    Supplier<SyncDelay> NONE = () -> () -> {
    };
}
