package io.smallrye.faulttolerance.retry.computed.delay;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.faulttolerance.api.ComputedDelayHandler;

@Dependent
public class MyComputedDelayHandler implements ComputedDelayHandler {
    static final Set<Integer> dependencyIds = ConcurrentHashMap.newKeySet();

    @Inject
    MyComputedDelayDependency dependency;

    @Override
    public Duration getDelay(ExecutionContext context) {
        dependencyIds.add(dependency.getId());
        Throwable failure = context.getFailure();
        if (failure instanceof RetryAfterException e) {
            return e.getRetryAfter();
        }
        if (failure instanceof IllegalStateException) {
            throw new RuntimeException("handler failed");
        }
        return null;
    }
}
