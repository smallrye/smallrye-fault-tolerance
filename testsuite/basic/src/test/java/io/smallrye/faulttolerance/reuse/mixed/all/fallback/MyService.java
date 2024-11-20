package io.smallrye.faulttolerance.reuse.mixed.all.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);
    static final AtomicInteger LONG_COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "helloFallback")
    public String hello() {
        STRING_COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }

    String helloFallback() {
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "theAnswerFallback")
    public CompletionStage<Integer> theAnswer() {
        INT_COUNTER.incrementAndGet();
        return failedFuture(new IllegalArgumentException());
    }

    CompletionStage<Integer> theAnswerFallback() {
        return completedFuture(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "badNumberFallback")
    public Uni<Long> badNumber() {
        LONG_COUNTER.incrementAndGet();
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    Uni<Long> badNumberFallback() {
        return Uni.createFrom().item(13L);
    }
}
