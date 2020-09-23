package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;

import org.eclipse.microprofile.context.ManagedExecutor;

@Dependent
@Alternative
@Priority(1)
public class TestAsyncExecutorProvider implements AsyncExecutorProvider {
    @Override
    public ExecutorService get() {
        return ManagedExecutor.builder().build();
    }
}
