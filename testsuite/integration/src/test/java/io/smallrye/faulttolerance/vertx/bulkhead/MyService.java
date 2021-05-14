package io.smallrye.faulttolerance.vertx.bulkhead;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.Vertx;

@ApplicationScoped
public class MyService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    @NonBlocking
    @Bulkhead(value = 3, waitingTaskQueue = 3)
    public CompletionStage<String> hello() {
        invocationThreads.add(Thread.currentThread().getName());

        CompletableFuture<String> result = new CompletableFuture<>();
        Vertx.currentContext().owner().setTimer(1000, ignored -> {
            invocationThreads.add(Thread.currentThread().getName());

            result.complete("Hello!");
        });
        return result;
    }
}
