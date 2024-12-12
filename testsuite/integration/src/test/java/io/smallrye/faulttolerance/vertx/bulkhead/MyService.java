package io.smallrye.faulttolerance.vertx.bulkhead;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.VertxContext;

@ApplicationScoped
public class MyService {
    static final Queue<ContextDescription> currentContexts = new ConcurrentLinkedQueue<>();

    @AsynchronousNonBlocking
    @Bulkhead(value = 3, waitingTaskQueue = 3)
    public CompletionStage<String> hello() {
        currentContexts.add(VertxContext.current().describe());

        CompletableFuture<String> result = new CompletableFuture<>();
        VertxContext.current().setTimer(1000, () -> {
            currentContexts.add(VertxContext.current().describe());

            result.complete("Hello!");
        });
        return result;
    }
}
