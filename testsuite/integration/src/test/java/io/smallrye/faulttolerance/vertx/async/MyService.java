package io.smallrye.faulttolerance.vertx.async;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.ExecutionStyle;
import io.smallrye.faulttolerance.vertx.VertxContext;
import io.vertx.core.Context;

@ApplicationScoped
public class MyService {
    static final Queue<ContextDescription> currentContexts = new ConcurrentLinkedQueue<>();

    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        currentContexts.add(VertxContext.current().describe());

        ExecutionStyle executionStyle;
        if (Context.isOnEventLoopThread()) {
            executionStyle = ExecutionStyle.WORKER;
        } else if (Context.isOnWorkerThread()) {
            executionStyle = ExecutionStyle.EVENT_LOOP;
        } else {
            throw new UnsupportedOperationException();
        }

        CompletableFuture<String> result = new CompletableFuture<>();
        VertxContext.current().setTimer(1000, () -> {
            currentContexts.add(VertxContext.current().describe());

            VertxContext.current().execute(executionStyle, () -> {
                result.complete("Hello!");
            });
        });
        return result;
    }
}
