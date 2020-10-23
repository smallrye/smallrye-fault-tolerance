package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

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
