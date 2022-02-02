package io.smallrye.faulttolerance.api;

import java.util.ServiceLoader;
import java.util.function.Function;

import io.smallrye.common.annotation.Experimental;

/**
 * This is an internal API. It may change incompatibly without notice.
 * It should not be used outside SmallRye Fault Tolerance.
 */
@Experimental("first attempt at providing programmatic API")
public final class FaultToleranceSpiAccess {
    private static class Holder {
        private static final FaultToleranceSpi INSTANCE = instantiateSpi();
    }

    public static <T, R> FaultTolerance.Builder<T, R> create(Function<FaultTolerance<T>, R> finisher) {
        return Holder.INSTANCE.newBuilder(finisher);
    }

    public static <T, R> FaultTolerance.Builder<T, R> createAsync(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        return Holder.INSTANCE.newAsyncBuilder(asyncType, finisher);
    }

    static CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return Holder.INSTANCE.circuitBreakerMaintenance();
    }

    private static FaultToleranceSpi instantiateSpi() {
        FaultToleranceSpi bestCandidate = null;
        int bestCandidatePriority = Integer.MIN_VALUE;
        for (FaultToleranceSpi candidate : ServiceLoader.load(FaultToleranceSpi.class)) {
            if (!candidate.applies()) {
                continue;
            }
            if (candidate.priority() > bestCandidatePriority) {
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            throw new IllegalStateException("Could not find implementation of FaultToleranceSpi, add a dependency on"
                    + " io.smallrye:smallrye-fault-tolerance or io.smallrye:smallrye-fault-tolerance-standalone");
        }
        return bestCandidate;
    }
}
