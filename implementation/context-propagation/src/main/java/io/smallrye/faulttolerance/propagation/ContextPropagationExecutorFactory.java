package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.ExecutorService;

import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.faulttolerance.ExecutorFactory;

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
    public int priority() {
        return 100;
    }
}
