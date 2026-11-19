package io.smallrye.faulttolerance.retry.computed.delay;

import java.util.function.Predicate;

public class IsRetryAfterResult implements Predicate<Object> {
    @Override
    public boolean test(Object result) {
        return result instanceof RetryAfterResult r && r.shouldRetry();
    }
}
