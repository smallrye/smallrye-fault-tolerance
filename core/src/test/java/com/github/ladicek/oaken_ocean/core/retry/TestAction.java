package com.github.ladicek.oaken_ocean.core.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class TestAction<V> implements Callable<V> {
    private final int initialFailuresCount;
    private final Supplier<? extends Exception> initialFailure;
    private final Callable<V> result;

    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public static <V> TestAction<V> immediatelyReturning(Callable<V> result) {
        return new TestAction<>(0, null, result);
    }

    public static <V> TestAction<V> initiallyFailing(int initialFailuresCount, Supplier<? extends Exception> initialFailure, Callable<V> result) {
        return new TestAction<>(initialFailuresCount, initialFailure, result);
    }

    private TestAction(int initialFailuresCount, Supplier<? extends Exception> initialFailure, Callable<V> result) {
        this.initialFailuresCount = initialFailuresCount;
        this.initialFailure = initialFailure;
        this.result = result;
    }

    @Override
    public V call() throws Exception {
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
