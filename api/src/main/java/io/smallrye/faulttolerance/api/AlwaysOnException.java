package io.smallrye.faulttolerance.api;

import java.util.function.Predicate;

public final class AlwaysOnException implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable ignored) {
        return true;
    }
}
