package io.smallrye.faulttolerance.mutiny.api;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.FaultToleranceSpiAccess;
import io.smallrye.mutiny.Uni;

/**
 * Contains factory methods for {@link FaultTolerance} where the type of value of the guarded action
 * is a Mutiny {@link Uni}. These actions are always asynchronous and may be offloaded to another thread
 * if necessary. In a modern reactive architecture, which is a typical use case for Mutiny, the actions
 * are non-blocking and thread offload is not necessary.
 * <p>
 * Note that {@code Uni} is a lazy type, so the guarded actions are not called until the guarded {@code Uni}
 * is subscribed to.
 */
public interface MutinyFaultTolerance {
    /**
     * Returns a builder that, at the end, returns a {@link Callable} guarding the given {@code action}.
     * The {@code action} is asynchronous and may be offloaded to another thread.
     * <p>
     * Note that {@code Uni} is a lazy type, so the action itself won't execute until the {@code Uni}
     * obtained from the resulting {@code Callable} is subscribed to.
     */
    static <T> FaultTolerance.Builder<Uni<T>, Callable<Uni<T>>> createCallable(Callable<Uni<T>> action) {
        return FaultToleranceSpiAccess.createAsync(Uni.class, ft -> ft.adaptCallable(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link Supplier} guarding the given {@code action}.
     * The {@code action} is asynchronous and may be offloaded to another thread.
     * <p>
     * Note that {@code Uni} is a lazy type, so the action itself won't execute until the {@code Uni}
     * obtained from the resulting {@code Supplier} is subscribed to.
     */
    static <T> FaultTolerance.Builder<Uni<T>, Supplier<Uni<T>>> createSupplier(Supplier<Uni<T>> action) {
        return FaultToleranceSpiAccess.createAsync(Uni.class, ft -> ft.adaptSupplier(action));
    }

    /**
     * Returns a builder that, at the end, returns a {@link FaultTolerance} object representing a set of configured
     * fault tolerance strategies. It can be used to execute asynchronous actions using {@link FaultTolerance#call(Callable)}
     * or {@link FaultTolerance#get(Supplier)}.
     * <p>
     * Note that {@code Uni} is a lazy type, so the action itself won't execute until the {@code Uni}
     * returned from the {@code call} or {@code get} methods is subscribed to. For this reason, using
     * {@link FaultTolerance#run(Runnable)} doesn't make sense, because there's no way to obtain
     * the resulting {@code Uni} that would need subscribing.
     * <p>
     * This method usually has to be called with an explicitly provided type argument. For example:
     * {@code MutinyFaultTolerance.&lt;String>create()}.
     */
    static <T> FaultTolerance.Builder<Uni<T>, FaultTolerance<Uni<T>>> create() {
        return FaultToleranceSpiAccess.createAsync(Uni.class, Function.identity());
    }
}
