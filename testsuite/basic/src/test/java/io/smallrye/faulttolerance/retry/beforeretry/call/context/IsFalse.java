package io.smallrye.faulttolerance.retry.beforeretry.call.context;

import java.util.function.Predicate;

public class IsFalse implements Predicate<Object> {
    @Override
    public boolean test(Object result) {
        return Boolean.FALSE.equals(result);
    }
}
