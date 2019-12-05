package io.smallrye.faulttolerance.core.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.FutureInvocationContext;

public final class FutureTestInvocation<V> implements FaultToleranceStrategy<Future<V>, FutureInvocationContext<V>> {
    private final int initialFailuresCount;
    private final Supplier<? extends Exception> initialFailure;
    private final Supplier<? extends RuntimeException> eventualFailure;
    private final Callable<Future<V>> result;

    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public static <V> FutureTestInvocation<V> immediatelyReturning(Callable<Future<V>> result) {
        return new FutureTestInvocation<V>(0, null, null, result);
    }

    public static <V> FutureTestInvocation<V> initiallyFailing(int initialFailuresCount,
            Supplier<? extends Exception> initialFailure,
            Callable<Future<V>> result) {
        return new FutureTestInvocation<>(initialFailuresCount, initialFailure, null, result);
    }

    public static <V> FutureTestInvocation<V> eventuallyFailing(Supplier<? extends RuntimeException> eventualFailure) {
        return new FutureTestInvocation<>(0, null, eventualFailure, null);
    }

    private FutureTestInvocation(int initialFailuresCount, Supplier<? extends Exception> initialFailure,
            Supplier<? extends RuntimeException> eventualFailure, Callable<Future<V>> result) {
        this.initialFailuresCount = initialFailuresCount;
        this.initialFailure = initialFailure;
        this.eventualFailure = eventualFailure;
        this.result = result;
    }

    @Override
    public Future<V> apply(FutureInvocationContext<V> target) throws Exception {
        int invocations = invocationCounter.incrementAndGet();

        if (initialFailuresCount > 0 && invocations <= initialFailuresCount) {
            throw initialFailure.get();
        }
        if (eventualFailure != null) {
            // mstodo simplify?
            return CompletableFuture.<V> supplyAsync(() -> {
                throw eventualFailure.get();
            }).toCompletableFuture();
        }

        return result.call();
    }

    public int numberOfInvocations() {
        return invocationCounter.get();
    }
}
