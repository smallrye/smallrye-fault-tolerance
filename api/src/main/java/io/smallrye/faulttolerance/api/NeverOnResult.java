package io.smallrye.faulttolerance.api;

import java.util.function.Predicate;

public final class NeverOnResult implements Predicate<Object> {
    @Override
    public boolean test(Object ignored) {
        return false;
    }
}
