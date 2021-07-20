package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.context.ManagedExecutor;

@Singleton
@Alternative
@Priority(1)
public class TestAsyncExecutorProvider implements AsyncExecutorProvider {
    @Override
    public ExecutorService get() {
        return ManagedExecutor.builder().build();
    }
}
