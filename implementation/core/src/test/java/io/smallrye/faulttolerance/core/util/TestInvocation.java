package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Callable;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final Callable<V> result;

    public static <V> TestInvocation<V> of(Callable<V> result) {
        return new TestInvocation<>(result);
    }

    private TestInvocation(Callable<V> result) {
        this.result = result;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        return result.call();
    }
}
