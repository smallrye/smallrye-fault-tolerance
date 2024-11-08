package io.smallrye.faulttolerance.mutiny.api;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.SpiAccess;
import io.smallrye.mutiny.Uni;

/**
 * @deprecated use {@link io.smallrye.faulttolerance.api.Guard} or {@link io.smallrye.faulttolerance.api.TypedGuard}
 */
@Deprecated(forRemoval = true)
public interface MutinyFaultTolerance {
    @Deprecated(forRemoval = true)
    static <T> FaultTolerance.Builder<Uni<T>, Callable<Uni<T>>> createCallable(Callable<Uni<T>> action) {
        return SpiAccess.get().newAsyncBuilder(Uni.class, ft -> ft.adaptCallable(action));
    }

    @Deprecated(forRemoval = true)
    static <T> FaultTolerance.Builder<Uni<T>, Supplier<Uni<T>>> createSupplier(Supplier<Uni<T>> action) {
        return SpiAccess.get().newAsyncBuilder(Uni.class, ft -> ft.adaptSupplier(action));
    }

    @Deprecated(forRemoval = true)
    static <T> FaultTolerance.Builder<Uni<T>, FaultTolerance<Uni<T>>> create() {
        return SpiAccess.get().newAsyncBuilder(Uni.class, Function.identity());
    }
}
