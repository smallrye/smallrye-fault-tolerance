package io.smallrye.faulttolerance.reuse.mixed.async.completionstage;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);

    @ApplyFaultTolerance("my-fault-tolerance")
    public CompletionStage<String> hello() {
        if (STRING_COUNTER.incrementAndGet() > 3) {
            return completedFuture("hello");
        }
        return failedFuture(new IllegalArgumentException());
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    public CompletionStage<Integer> theAnswer() {
        if (INT_COUNTER.incrementAndGet() > 3) {
            return completedFuture(42);
        }
        return failedFuture(new IllegalArgumentException());
    }
}
