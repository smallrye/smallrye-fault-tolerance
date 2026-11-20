package io.smallrye.faulttolerance.retry.backoff.custom.compat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.smallrye.faulttolerance.api.CustomBackoffStrategy;

public class TestBackoffStrategy implements CustomBackoffStrategy {
    static final List<Class<? extends Throwable>> exceptions = new CopyOnWriteArrayList<>();
    static long initialDelay = -1;

    static void clear() {
        initialDelay = -1;
        exceptions.clear();
    }

    @Override
    public void init(long initialDelayInMillis) {
        initialDelay = initialDelayInMillis;
    }

    @Override
    @SuppressWarnings("removal")
    public long nextDelayInMillis(Throwable exception) {
        exceptions.add(exception != null ? exception.getClass() : null);
        return 5;
    }
}
