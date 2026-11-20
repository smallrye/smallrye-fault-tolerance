package io.smallrye.faulttolerance.retry.backoff.custom;

import java.util.function.Predicate;

public class IsNotString implements Predicate<Object> {
    @Override
    public boolean test(Object o) {
        return !(o instanceof String);
    }
}
