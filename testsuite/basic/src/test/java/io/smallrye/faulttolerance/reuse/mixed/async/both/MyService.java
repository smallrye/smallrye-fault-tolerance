package io.smallrye.faulttolerance.reuse.mixed.async.both;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> hello() {
        if (STRING_COUNTER.incrementAndGet() > 3) {
            return completedFuture("hello");
        }
        return failedFuture(new IllegalArgumentException());
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<Integer> theAnswer() {
        if (INT_COUNTER.incrementAndGet() > 3) {
            return Uni.createFrom().item(42);
        }
        return Uni.createFrom().failure(new IllegalArgumentException());
    }
}
