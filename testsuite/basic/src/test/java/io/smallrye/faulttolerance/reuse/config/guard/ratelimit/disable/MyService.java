package io.smallrye.faulttolerance.reuse.config.guard.ratelimit.disable;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> helloCompletionStage() {
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<String> helloUni() {
        return Uni.createFrom().item("hello");
    }
}
