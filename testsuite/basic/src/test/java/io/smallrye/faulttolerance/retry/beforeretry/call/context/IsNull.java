package io.smallrye.faulttolerance.retry.beforeretry.call.context;

import java.util.function.Predicate;

public class IsNull implements Predicate<Object> {
    @Override
    public boolean test(Object o) {
        return o == null;
    }
}
