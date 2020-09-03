package io.smallrye.faulttolerance.propagation;

import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.context.ManagedExecutor;

import io.smallrye.faulttolerance.ExecutorFactory;

public class ContextPropagationExecutorFactory implements ExecutorFactory {
    @Override
    public ExecutorService createCoreExecutor(int size) {
        return ManagedExecutor.builder().maxAsync(size).build();
    }

    @Override
    public int priority() {
        return 100;
    }
}
