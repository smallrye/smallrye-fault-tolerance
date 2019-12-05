package io.smallrye.faulttolerance.core.timeout;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public final class FutureTestInvocation<V> implements FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> {
    private final Barrier startBarrier;
    private final Barrier delayBarrier;
    private final Callable<Future<V>> result;

    public static <V> FutureTestInvocation<V> immediatelyReturning(Callable<Future<V>> result) {
        return new FutureTestInvocation<V>(null, null, result);
    }

    public static <V> FutureTestInvocation<V> delayed(Barrier delayBarrier, Callable<Future<V>> result) {
        return new FutureTestInvocation<>(null, delayBarrier, result);
    }

    public static <V> FutureTestInvocation<V> delayed(Barrier startBarrier, Barrier delayBarrier, Callable<Future<V>> result) {
        return new FutureTestInvocation<>(startBarrier, delayBarrier, result);
    }

    private FutureTestInvocation(Barrier startBarrier, Barrier delayBarrier, Callable<Future<V>> result) {
        this.startBarrier = startBarrier;
        this.delayBarrier = delayBarrier;
        this.result = result;
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }

        if (delayBarrier != null) {
            delayBarrier.await();
        }

        return result.call();
    }
}
