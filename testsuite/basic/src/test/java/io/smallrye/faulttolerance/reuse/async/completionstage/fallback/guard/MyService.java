package io.smallrye.faulttolerance.reuse.async.completionstage.fallback.guard;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        COUNTER.incrementAndGet();
        return failedFuture(new IllegalArgumentException());
    }

    CompletionStage<String> fallback() {
        return completedFuture("better fallback");
    }
}
