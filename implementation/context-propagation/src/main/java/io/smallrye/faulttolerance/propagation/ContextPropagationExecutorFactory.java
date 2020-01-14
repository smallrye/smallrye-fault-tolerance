package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.faulttolerance.ExecutorFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ContextPropagationExecutorFactory implements ExecutorFactory {

    @Override
    public ExecutorService createCoreExecutor(int size) {
        return ManagedExecutor.builder().maxAsync(size).build();
    }

    @Override
    public ExecutorService createExecutor(int coreSize, int size) {
        SmallRyeManagedExecutor.Builder builder = (SmallRyeManagedExecutor.Builder) ManagedExecutor.builder();
        return builder.maxAsync(size).build();
    }

    @Override
    public ScheduledExecutorService createTimeoutExecutor(int size) {
        return Executors.newScheduledThreadPool(size);
    }

    @Override
    public int priority() {
        return 100;
    }
}
