package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.FutureInvocationContext;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;
import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class FutureTestInvocation<V> implements FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>>{
    private final Barrier startBarrier;
    private final Barrier endBarrier;
    private final Callable<Future<V>> result;

    public static <V> FutureTestInvocation<V> immediatelyReturning(Callable<Future<V>> result) {
        return new FutureTestInvocation<>(null, null, result);
    }

    public static <V> FutureTestInvocation<V> waitingOnBarrier(Barrier startBarrier, Barrier endBarrier, Callable<Future<V>> result) {
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
