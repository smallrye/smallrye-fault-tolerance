package io.smallrye.faulttolerance.core.fallback;

import java.util.concurrent.Callable;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.InvocationContext;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final Barrier startBarrier;
    private final Barrier endBarrier;
    private final Callable<V> result;

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(null, null, result);
    }

    public static <V> TestInvocation<V> waitingOnBarrier(Barrier startBarrier, Barrier endBarrier, Callable<V> result) {
        return new TestInvocation<>(startBarrier, endBarrier, result);
    }

    private TestInvocation(Barrier startBarrier, Barrier endBarrier, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.endBarrier = endBarrier;
        this.result = result;
    }

    @Override
    public V apply(InvocationContext<V> ctx) throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }
        if (endBarrier != null) {
            endBarrier.await();
        }

        return result.call();
    }
}
