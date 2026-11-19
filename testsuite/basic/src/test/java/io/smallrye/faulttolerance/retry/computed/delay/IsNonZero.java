package io.smallrye.faulttolerance.retry.computed.delay;

import java.util.function.Predicate;

public class IsNonZero implements Predicate<Object> {
    @Override
    public boolean test(Object result) {
        return result instanceof Integer millis && millis != 0;
    }
}
