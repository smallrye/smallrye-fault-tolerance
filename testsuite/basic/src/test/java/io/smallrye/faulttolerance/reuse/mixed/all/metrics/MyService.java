package io.smallrye.faulttolerance.reuse.mixed.all.metrics;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    private static final AtomicInteger counter = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    public String first() {
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<Integer> second() {
        if (counter.incrementAndGet() > 3) {
            return completedFuture(42);
        }
        return failedFuture(new IllegalArgumentException());
    }

    public void resetSecondCounter() {
        counter.set(0);
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<Long> third() {
        return Uni.createFrom().failure(new IllegalArgumentException());
    }
}
