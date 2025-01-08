package io.smallrye.faulttolerance.reuse.config.guard.circuit.breaker;

import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        throw new IllegalStateException();
    }

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<Integer> helloCompletionStage() {
        return failedFuture(new IllegalArgumentException());
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<Long> helloUni() {
        return Uni.createFrom().failure(new IllegalArgumentException());
    }
}
