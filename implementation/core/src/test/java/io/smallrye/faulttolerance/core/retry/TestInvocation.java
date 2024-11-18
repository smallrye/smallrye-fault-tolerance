package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public final class TestInvocation<V> implements FaultToleranceStrategy<V> {
    private final int initialFailuresCount;
    private final Supplier<? extends Exception> initialFailure;
    private final Callable<V> result;

    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public static <V> TestInvocation<V> immediatelyReturning(Callable<V> result) {
        return new TestInvocation<>(0, null, result);
    }

    public static <V> TestInvocation<V> initiallyFailing(int initialFailuresCount,
            Supplier<? extends Exception> initialFailure, Callable<V> result) {
        return new TestInvocation<>(initialFailuresCount, initialFailure, result);
    }

    private TestInvocation(int initialFailuresCount, Supplier<? extends Exception> initialFailure, Callable<V> result) {
        this.initialFailuresCount = initialFailuresCount;
        this.initialFailure = initialFailure;
        this.result = result;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        int invocations = invocationCounter.incrementAndGet();

        if (initialFailuresCount > 0 && invocations <= initialFailuresCount) {
            return Future.ofError(initialFailure.get());
        }

        return Future.from(result);
    }

    public int numberOfInvocations() {
        return invocationCounter.get();
    }
}
