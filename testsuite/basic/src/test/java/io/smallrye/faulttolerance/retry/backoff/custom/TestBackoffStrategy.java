package io.smallrye.faulttolerance.retry.backoff.custom;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.faulttolerance.api.CustomBackoffStrategy;

public class TestBackoffStrategy implements CustomBackoffStrategy {
    static long initialDelay = -1;
    static List<Class<? extends Throwable>> exceptions = new ArrayList<>();

    @Override
    public void init(long initialDelayInMillis) {
        initialDelay = initialDelayInMillis;
    }

    @Override
    public long nextDelayInMillis(Throwable exception) {
        exceptions.add(exception.getClass());
        return 250;
    }
}
