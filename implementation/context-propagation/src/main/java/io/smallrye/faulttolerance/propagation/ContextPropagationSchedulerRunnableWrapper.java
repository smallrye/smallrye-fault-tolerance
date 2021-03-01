package io.smallrye.faulttolerance.propagation;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.faulttolerance.core.scheduler.SchedulerRunnableWrapper;

public class ContextPropagationSchedulerRunnableWrapper implements SchedulerRunnableWrapper {
    private final ThreadContext threadContext = ThreadContext.builder().build();

    @Override
    public Runnable wrap(Runnable runnable) {
        return threadContext.contextualRunnable(runnable);
    }
}
