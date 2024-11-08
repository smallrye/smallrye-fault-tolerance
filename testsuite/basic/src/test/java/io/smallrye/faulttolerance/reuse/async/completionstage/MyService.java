package io.smallrye.faulttolerance.reuse.async.completionstage;

import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> hello() {
        COUNTER.incrementAndGet();
        return failedFuture(new IllegalArgumentException());
    }
}
