package io.smallrye.faulttolerance.context.propagation;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.faulttolerance.core.util.RunnableWrapper;

public class ContextPropagationRunnableWrapper implements RunnableWrapper {
    private final ThreadContext threadContext = ThreadContext.builder().build();

    @Override
    public Runnable wrap(Runnable runnable) {
        return threadContext.contextualRunnable(runnable);
    }
}
