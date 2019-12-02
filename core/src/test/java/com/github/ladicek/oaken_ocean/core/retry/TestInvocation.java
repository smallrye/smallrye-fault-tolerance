package com.github.ladicek.oaken_ocean.core.retry;

import com.github.ladicek.oaken_ocean.core.FaultToleranceStrategy;
import com.github.ladicek.oaken_ocean.core.SimpleInvocationContext;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class TestInvocation<V> implements FaultToleranceStrategy<V, SimpleInvocationContext<V>> {
    private final int initialFailuresCount;
    private final Supplier<? extends Exception> initialFailure;
    private final Callable<V> result;

    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(0, null, result);
    }

    public static <V> TestInvocation<V> initiallyFailing(int initialFailuresCount, Supplier<? extends Exception> initialFailure, Callable<V> result) {
        return new TestInvocation<>(initialFailuresCount, initialFailure, result);
    }

    private TestInvocation(int initialFailuresCount, Supplier<? extends Exception> initialFailure, Callable<V> result) {
        this.initialFailuresCount = initialFailuresCount;
        this.initialFailure = initialFailure;
        this.result = result;
    }

    @Override
    public V apply(SimpleInvocationContext<V> target) throws Exception {
        int invocations = invocationCounter.incrementAndGet();

        if (initialFailuresCount > 0 && invocations <= initialFailuresCount) {
            throw initialFailure.get();
        }

        return result.call();
    }

    public int numberOfInvocations() {
        return invocationCounter.get();
    }
}
