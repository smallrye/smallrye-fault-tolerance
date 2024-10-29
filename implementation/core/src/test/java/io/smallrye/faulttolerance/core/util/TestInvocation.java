package io.smallrye.faulttolerance.core.util;

import java.util.concurrent.Callable;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final Callable<V> result;

    public static <V> TestInvocation<V> of(Callable<V> result) {
        return new TestInvocation<>(result);
    }

    private TestInvocation(Callable<V> result) {
        this.result = result;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        return Future.from(result);
    }
}
