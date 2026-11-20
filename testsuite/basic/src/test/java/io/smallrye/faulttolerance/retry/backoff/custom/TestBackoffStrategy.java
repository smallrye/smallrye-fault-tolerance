package io.smallrye.faulttolerance.retry.backoff.custom;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.faulttolerance.api.CustomBackoffStrategy;

public class TestBackoffStrategy implements CustomBackoffStrategy {
    static long initialDelay = -1;
    static List<Class<?>> results = new ArrayList<>();
    static List<Class<? extends Throwable>> exceptions = new ArrayList<>();

    static void clear() {
        initialDelay = -1;
        results.clear();
        exceptions.clear();
    }

    @Override
    public void init(long initialDelayInMillis) {
        initialDelay = initialDelayInMillis;
    }

    @Override
    public long nextDelayInMillis(Object result, Throwable exception) {
        results.add(result != null ? result.getClass() : null);
        exceptions.add(exception != null ? exception.getClass() : null);
        return 250;
    }
}
