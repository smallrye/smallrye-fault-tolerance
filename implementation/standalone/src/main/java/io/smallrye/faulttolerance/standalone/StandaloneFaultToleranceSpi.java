package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.FaultToleranceSpi;
import io.smallrye.faulttolerance.core.apiimpl.FaultToleranceImpl;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.Timer;

public class StandaloneFaultToleranceSpi implements FaultToleranceSpi {
    static class Dependencies {
        boolean ftEnabled = !"false".equals(System.getProperty("MP_Fault_Tolerance_NonFallback_Enabled"));

        // TODO let users integrate their own thread pool
        final Executor executor = Executors.newCachedThreadPool();
        final Timer timer = new Timer(executor);
        final EventLoop eventLoop = EventLoop.get();

        final StandaloneCircuitBreakerMaintenance cbMaintenance = new StandaloneCircuitBreakerMaintenance();
    }

    static class DependenciesHolder {
        static final Dependencies INSTANCE = new Dependencies();
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
        Dependencies deps = DependenciesHolder.INSTANCE;
        return new FaultToleranceImpl.BuilderImpl<>(deps.ftEnabled, deps.executor, deps.timer, deps.eventLoop,
                deps.cbMaintenance, false, null, finisher);
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        Dependencies deps = DependenciesHolder.INSTANCE;
        return new FaultToleranceImpl.BuilderImpl<>(deps.ftEnabled, deps.executor, deps.timer, deps.eventLoop,
                deps.cbMaintenance, true, asyncType, finisher);
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return DependenciesHolder.INSTANCE.cbMaintenance;
    }
}
