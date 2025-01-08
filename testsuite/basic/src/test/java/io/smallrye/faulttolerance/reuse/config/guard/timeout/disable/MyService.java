package io.smallrye.faulttolerance.reuse.config.guard.timeout.disable;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() throws InterruptedException {
        Thread.sleep(3_000);
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public CompletionStage<String> helloCompletionStage() throws InterruptedException {
        Thread.sleep(3_000);
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public Uni<String> helloUni() throws InterruptedException {
        Thread.sleep(3_000);
        return Uni.createFrom().item("hello");
    }
}
