package io.smallrye.faulttolerance.core.fallback;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public final class FutureTestInvocation<V> implements FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> {
    private final Barrier startBarrier;
    private final Barrier endBarrier;
    private final Callable<Future<V>> result;

    public static <V> FutureTestInvocation<V> immediatelyReturning(Callable<Future<V>> result) {
        return new FutureTestInvocation<>(null, null, result);
    }

    public static <V> FutureTestInvocation<V> waitingOnBarrier(Barrier startBarrier, Barrier endBarrier,
            Callable<Future<V>> result) {
        return new FutureTestInvocation<>(startBarrier, endBarrier, result);
    }

    private FutureTestInvocation(Barrier startBarrier, Barrier endBarrier, Callable<Future<V>> result) {
        this.startBarrier = startBarrier;
        this.endBarrier = endBarrier;
        this.result = result;
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }
        if (endBarrier != null) {
            endBarrier.await();
        }

        return result.call();
    }
}
