package io.smallrye.faulttolerance.reuse.async.completionstage.metrics;

import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> first() {
        return failedFuture(new IllegalArgumentException());
    }

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> second() {
        return failedFuture(new IllegalStateException());
    }
}
