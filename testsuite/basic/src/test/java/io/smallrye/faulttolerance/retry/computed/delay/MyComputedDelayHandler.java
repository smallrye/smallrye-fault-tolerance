package io.smallrye.faulttolerance.retry.computed.delay;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.smallrye.faulttolerance.api.CallContext;
import io.smallrye.faulttolerance.api.ComputedDelayHandler;

@Dependent
public class MyComputedDelayHandler implements ComputedDelayHandler {
    static final Set<Integer> dependencyIds = ConcurrentHashMap.newKeySet();
    static final List<Object> observedResults = new CopyOnWriteArrayList<>();
    static final List<Throwable> observedFailures = new CopyOnWriteArrayList<>();

    @Inject
    MyComputedDelayDependency dependency;

    @Override
    public Duration getDelay(CallContext context) {
        dependencyIds.add(dependency.getId());
        observedResults.add(context.getResult());
        observedFailures.add(context.getFailure());
        if (context.getResult() instanceof RetryAfterResult r) {
            return r.getRetryAfter();
        }
        if (context.getResult() instanceof Integer millis) {
            // a primitive result reaches the handler boxed
            return Duration.ofMillis(millis);
        }
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
