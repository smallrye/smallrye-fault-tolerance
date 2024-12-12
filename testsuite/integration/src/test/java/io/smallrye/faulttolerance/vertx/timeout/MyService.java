package io.smallrye.faulttolerance.vertx.timeout;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.VertxContext;

@ApplicationScoped
public class MyService {
    static final Queue<ContextDescription> currentContexts = new ConcurrentLinkedQueue<>();

    @Timeout
    @AsynchronousNonBlocking
    public CompletionStage<String> hello(long sleep) {
        currentContexts.add(VertxContext.current().describe());

        CompletableFuture<String> result = new CompletableFuture<>();
        VertxContext.current().setTimer(sleep, () -> {
            currentContexts.add(VertxContext.current().describe());
            result.complete("Hello!");
        });
        return result;
    }
}
