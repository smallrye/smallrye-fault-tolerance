package io.smallrye.faulttolerance;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.Spi;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.apiimpl.BasicCircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.core.apiimpl.BuilderEagerDependencies;
import io.smallrye.faulttolerance.core.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.core.apiimpl.FaultToleranceImpl;
import io.smallrye.faulttolerance.core.apiimpl.GuardImpl;
import io.smallrye.faulttolerance.core.apiimpl.TypedGuardImpl;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.metrics.MetricsProvider;
import io.smallrye.faulttolerance.core.timer.Timer;

public class CdiSpi implements Spi {
    @Singleton
    public static class EagerDependencies implements BuilderEagerDependencies {
        @Inject
        CircuitBreakerMaintenanceImpl cbMaintenance;

        @Override
        public BasicCircuitBreakerMaintenanceImpl cbMaintenance() {
            return cbMaintenance;
        }
    }

    @Singleton
    public static class LazyDependencies implements BuilderLazyDependencies {
        @Inject
        Enablement enablement;

        @Inject
        ExecutorHolder executorHolder;

        @Inject
        MetricsProvider metricsProvider;

        @Override
        public boolean ftEnabled() {
            return enablement.ft();
        }

        @Override
        public ExecutorService asyncExecutor() {
            return executorHolder.getAsyncExecutor();
        }

        @Override
        public EventLoop eventLoop() {
            return executorHolder.getEventLoop();
        }

        @Override
        public Timer timer() {
            return executorHolder.getTimer();
        }

        @Override
        public MetricsProvider metricsProvider() {
            return metricsProvider;
        }
    }

    private BuilderEagerDependencies eagerDependencies() {
        // always lookup from current CDI container, because there's no guarantee that `CDI.current()` will always be
        // the same (in certain environments, e.g. in the test suite, CDI containers come and go freely)
        return CDI.current().select(EagerDependencies.class).get();
    }

    private BuilderLazyDependencies lazyDependencies() {
        // always lookup from current CDI container, because there's no guarantee that `CDI.current()` will always be
        // the same (in certain environments, e.g. in the test suite, CDI containers come and go freely)
        return CDI.current().select(LazyDependencies.class).get();
    }

    @Override
    public boolean applies() {
        try {
            CDI.current();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public Guard.Builder newGuardBuilder() {
        return new GuardImpl.BuilderImpl(eagerDependencies(), this::lazyDependencies);
    }

    @Override
    public <T> TypedGuard.Builder<T> newTypedGuardBuilder(Type type) {
        return new TypedGuardImpl.BuilderImpl<>(eagerDependencies(), this::lazyDependencies, type);
    }

    @Override
    @Deprecated(forRemoval = true)
    public <T, R> FaultTolerance.Builder<T, R> newBuilder(Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(eagerDependencies(), this::lazyDependencies, null, finisher);
    }

    @Override
    @Deprecated(forRemoval = true)
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(eagerDependencies(), this::lazyDependencies, asyncType, finisher);
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return eagerDependencies().cbMaintenance();
    }
}
