package io.smallrye.faulttolerance.standalone;

import java.lang.reflect.Type;
import java.util.function.Function;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.Spi;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.apiimpl.FaultToleranceImpl;
import io.smallrye.faulttolerance.apiimpl.GuardImpl;
import io.smallrye.faulttolerance.apiimpl.TypedGuardImpl;

public class StandaloneSpi implements Spi {
    static class EagerDependenciesHolder {
        static final EagerDependencies INSTANCE = new EagerDependencies();
    }

    static class LazyDependenciesHolder {
        static final BuilderLazyDependencies INSTANCE = StandaloneFaultTolerance.getLazyDependencies();
    }

    @Override
    public boolean applies() {
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Guard.Builder newGuardBuilder() {
        return new GuardImpl.BuilderImpl(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE);
    }

    @Override
    public <T> TypedGuard.Builder<T> newTypedGuardBuilder(Type type) {
        return new TypedGuardImpl.BuilderImpl<>(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE,
                type);
    }

    @Override
    @Deprecated(forRemoval = true)
    public <T, R> FaultTolerance.Builder<T, R> newBuilder(Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE,
                null, finisher);
    }

    @Override
    @Deprecated(forRemoval = true)
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE,
                asyncType, finisher);
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return EagerDependenciesHolder.INSTANCE.cbMaintenance;
    }
}
