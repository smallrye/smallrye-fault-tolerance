package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.FaultToleranceSpi;
import io.smallrye.faulttolerance.core.apiimpl.FaultToleranceImpl;
import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.Timer;

public class CdiFaultToleranceSpi implements FaultToleranceSpi {
    @Singleton
    static class Dependencies {
        @Inject
        @ConfigProperty(name = "MP_Fault_Tolerance_NonFallback_Enabled", defaultValue = "true")
        boolean ftEnabled;

        @Inject
        ExecutorHolder executorHolder;

        @Inject
        CircuitBreakerMaintenanceImpl cbMaintenance;

        ExecutorService asyncExecutor() {
            return executorHolder.getAsyncExecutor();
        }

        EventLoop eventLoop() {
            return executorHolder.getEventLoop();
        }

        Timer timer() {
            return executorHolder.getTimer();
        }
    }

    private Dependencies getDependencies() {
        // always lookup from current CDI container, because there's no guarantee that `CDI.current()` will always be
        // the same (in certain environments, e.g. in the test suite, CDI containers come and go freely)
        return CDI.current().select(Dependencies.class).get();
    }

    @Override
    public boolean applies() {
        try {
            return getDependencies() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newBuilder(Function<FaultTolerance<T>, R> finisher) {
        Dependencies deps = getDependencies();
        return new FaultToleranceImpl.BuilderImpl<>(deps.ftEnabled, deps.asyncExecutor(), deps.timer(), deps.eventLoop(),
                deps.cbMaintenance, false, null, finisher);
    }

    @Override
    public <T, R> FaultTolerance.Builder<T, R> newAsyncBuilder(Class<?> asyncType, Function<FaultTolerance<T>, R> finisher) {
        Dependencies deps = getDependencies();
        return new FaultToleranceImpl.BuilderImpl<>(deps.ftEnabled, deps.asyncExecutor(), deps.timer(), deps.eventLoop(),
                deps.cbMaintenance, true, asyncType, finisher);
    }

    @Override
    public CircuitBreakerMaintenance circuitBreakerMaintenance() {
        return getDependencies().cbMaintenance;
    }
}
