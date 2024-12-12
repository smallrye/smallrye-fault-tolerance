package io.smallrye.faulttolerance.vertx.retry.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.VertxContext;

@ApplicationScoped
public class MyService {
    static final Queue<ContextDescription> currentContexts = new ConcurrentLinkedQueue<>();

    @AsynchronousNonBlocking
    @Retry(maxRetries = 10, delay = 5, delayUnit = ChronoUnit.MILLIS, jitter = 0)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        currentContexts.add(VertxContext.current().describe());
        return failedFuture(new Exception());
    }

    public CompletionStage<String> fallback() {
        currentContexts.add(VertxContext.current().describe());
        return completedFuture("Hello!");
    }
}
