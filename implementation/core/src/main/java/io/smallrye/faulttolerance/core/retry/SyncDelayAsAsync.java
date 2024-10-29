package io.smallrye.faulttolerance.core.retry;

import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.util.concurrent.Executor;

/**
 * A {@link SyncDelay} adapter to {@link AsyncDelay}. The passed {@code Executor}
 * is ignored; the implementation just blocks the executing thread for given time
 * and then execute the task directly.
 */
final class SyncDelayAsAsync implements AsyncDelay {
    private final SyncDelay delegate;

    public SyncDelayAsAsync(SyncDelay delegate) {
        this.delegate = checkNotNull(delegate, "SyncDelay must be set");
    }

    @Override
    public void after(Throwable cause, Runnable task, Executor executor) {
        try {
            delegate.sleep(cause);
        } catch (InterruptedException e) {
            throw sneakyThrow(e);
        }
        task.run();
    }
}
