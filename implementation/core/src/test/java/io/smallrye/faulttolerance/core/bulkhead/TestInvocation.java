package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Callable;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final Barrier delayBarrier;
    private final Callable<V> result;

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(null, result);
    }

    public static <V> TestInvocation<V> delayed(Barrier delayBarrier, Callable<V> result) {
        return new TestInvocation<>(delayBarrier, result);
    }

    private TestInvocation(Barrier delayBarrier, Callable<V> result) {
        this.delayBarrier = delayBarrier;
        this.result = result;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (delayBarrier != null) {
            delayBarrier.await();
        }

        return result.call();
    }
}
