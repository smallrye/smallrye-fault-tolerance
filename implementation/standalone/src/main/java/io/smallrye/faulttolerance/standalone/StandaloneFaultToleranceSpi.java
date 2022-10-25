package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.FaultToleranceSpi;
import io.smallrye.faulttolerance.core.apiimpl.BasicCircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.core.apiimpl.BuilderEagerDependencies;
import io.smallrye.faulttolerance.core.apiimpl.BuilderLazyDependencies;
import io.smallrye.faulttolerance.core.apiimpl.FaultToleranceImpl;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;

public class StandaloneFaultToleranceSpi implements FaultToleranceSpi {
    static class EagerDependencies implements BuilderEagerDependencies {
        final BasicCircuitBreakerMaintenanceImpl cbMaintenance = new BasicCircuitBreakerMaintenanceImpl();

        @Override
        public BasicCircuitBreakerMaintenanceImpl cbMaintenance() {
            return cbMaintenance;
        }
    }

    static class LazyDependencies implements BuilderLazyDependencies {
        boolean ftEnabled = !"false".equals(System.getProperty("MP_Fault_Tolerance_NonFallback_Enabled"));

        // TODO let users integrate their own thread pool
        final ExecutorService executor = Executors.newCachedThreadPool();
        final EventLoop eventLoop = EventLoop.get();
        final Timer timer = new ThreadTimer(executor);

        @Override
        public boolean ftEnabled() {
            return ftEnabled;
        }

        @Override
        public ExecutorService asyncExecutor() {
            return executor;
        }

        @Override
        public EventLoop eventLoop() {
            return eventLoop;
        }

        @Override
        public Timer timer() {
            return timer;
        }
    }

    static class EagerDependenciesHolder {
        static final EagerDependencies INSTANCE = new EagerDependencies();
    }

    static class LazyDependenciesHolder {
        static final LazyDependencies INSTANCE = new LazyDependencies();
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
    public <T, R> FaultTolerance.Builder<T, R> newBuilder(Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE,
                null, finisher);
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        return new FaultToleranceImpl.BuilderImpl<>(EagerDependenciesHolder.INSTANCE, () -> LazyDependenciesHolder.INSTANCE,
                asyncType, finisher);
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return EagerDependenciesHolder.INSTANCE.cbMaintenance;
    }
}
