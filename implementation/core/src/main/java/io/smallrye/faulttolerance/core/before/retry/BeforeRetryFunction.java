package io.smallrye.faulttolerance.core.before.retry;

import io.smallrye.faulttolerance.core.InvocationContext;

@FunctionalInterface
public interface BeforeRetryFunction<T> {
    void call(InvocationContext<T> invocationContext) throws Exception;
}