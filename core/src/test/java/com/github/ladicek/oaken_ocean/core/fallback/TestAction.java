package com.github.ladicek.oaken_ocean.core.fallback;

import com.github.ladicek.oaken_ocean.core.util.barrier.Barrier;

import java.util.concurrent.Callable;

public final class TestAction<V> implements Callable<V> {
    private final Barrier startBarrier;
    private final Barrier endBarrier;
    private final Callable<V> result;

    public static <V> TestAction<V> immediatelyReturning(Callable<V> result) {
        return new TestAction<>(null, null, result);
    }

    public static <V> TestAction<V> waitingOnBarrier(Barrier startBarrier, Barrier endBarrier, Callable<V> result) {
        return new TestAction<>(startBarrier, endBarrier, result);
    }

    private TestAction(Barrier startBarrier, Barrier endBarrier, Callable<V> result) {
        this.startBarrier = startBarrier;
        this.endBarrier = endBarrier;
        this.result = result;
    }

    @Override
    public V call() throws Exception {
        if (startBarrier != null) {
            startBarrier.open();
        }
        if (endBarrier != null) {
            endBarrier.await();
        }

        return result.call();
    }
}
