package io.smallrye.faulttolerance.core.retry;

import java.util.function.Supplier;

/**
 * Performs a delay synchronously. That is, blocks the calling thread for the duration of the delay.
 */
public interface SyncDelay {
    void sleep(Throwable cause) throws InterruptedException;

    Supplier<SyncDelay> NONE = () -> cause -> {
    };
}
