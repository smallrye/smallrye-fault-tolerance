package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.faulttolerance.ExecutorFactory;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ContextPropagationExecutorFactory implements ExecutorFactory {

    @Override
    public ExecutorService createCoreExecutor(int size) {
        return SmallRyeManagedExecutor.builder().withNewExecutorService().maxAsync(size).build();
    }

    @Override
    public ExecutorService createExecutor(int coreSize, int size) {
        return SmallRyeManagedExecutor.builder().withNewExecutorService().maxAsync(size).build();
    }

    @Override
    public ScheduledExecutorService createTimeoutExecutor(int size) {
        ThreadContext threadContext = ThreadContext.builder().build();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(size);
        return new ContextPropagatingScheduledExecutorService(threadContext, executor);
    }

    @Override
    public int priority() {
        return 100;
    }
}
