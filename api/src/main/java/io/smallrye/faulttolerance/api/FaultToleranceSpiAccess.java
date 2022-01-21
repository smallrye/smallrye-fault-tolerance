package io.smallrye.faulttolerance.api;

import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class FaultToleranceSpiAccess {
    static class Holder {
        static final FaultToleranceSpi INSTANCE = instantiateSpi();
    }

    static <T, R> FaultTolerance.Builder<T, R> create(Function<FaultTolerance<T>, R> finisher) {
        return Holder.INSTANCE.newBuilder(finisher);
    };

    static <T, R> FaultTolerance.Builder<CompletionStage<T>, R> createAsync(
            Function<FaultTolerance<CompletionStage<T>>, R> finisher) {
        return Holder.INSTANCE.newAsyncBuilder(finisher);
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
