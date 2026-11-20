package io.smallrye.faulttolerance.retry.beforeretry.call.context;

import java.util.function.Predicate;

public class IsBad implements Predicate<Object> {
    @Override
    public boolean test(Object result) {
        return "bad".equals(result);
    }
}
