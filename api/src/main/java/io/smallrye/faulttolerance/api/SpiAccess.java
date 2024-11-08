package io.smallrye.faulttolerance.api;

import java.util.ServiceLoader;

import io.smallrye.common.annotation.Experimental;

/**
 * This is an internal API. It may change incompatibly without notice.
 * It should not be used outside SmallRye Fault Tolerance.
 */
@Experimental("first attempt at providing programmatic API")
public final class SpiAccess {
    private static class Holder {
        private static final Spi INSTANCE = instantiateSpi();
    }

    public static Spi get() {
        return Holder.INSTANCE;
    }

    private static Spi instantiateSpi() {
        Spi bestCandidate = null;
        int bestCandidatePriority = Integer.MIN_VALUE;
        for (Spi candidate : ServiceLoader.load(Spi.class)) {
            if (!candidate.applies()) {
                continue;
            }
            if (candidate.priority() > bestCandidatePriority) {
                bestCandidate = candidate;
                bestCandidatePriority = candidate.priority();
            }
        }

        if (bestCandidate == null) {
            throw new IllegalStateException("Could not find implementation of io.smallrye.faulttolerance.api.Spi, add"
                    + " a dependency on io.smallrye:smallrye-fault-tolerance or io.smallrye:smallrye-fault-tolerance-standalone");
        }
        return bestCandidate;
    }
}
