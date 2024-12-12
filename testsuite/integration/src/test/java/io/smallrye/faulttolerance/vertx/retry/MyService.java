package io.smallrye.faulttolerance.vertx.retry;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.vertx.ContextDescription;
import io.smallrye.faulttolerance.vertx.VertxContext;

@ApplicationScoped
public class MyService {
    static final Queue<ContextDescription> currentContexts = new ConcurrentLinkedQueue<>();

    @AsynchronousNonBlocking
    @Retry(maxRetries = 20, delay = 5, delayUnit = ChronoUnit.MILLIS, jitter = 0)
    public CompletionStage<String> hello(AtomicInteger counter) {
        currentContexts.add(VertxContext.current().describe());

        int current = counter.incrementAndGet();
        if (current > 10) {
            return completedFuture("Hello!");
        }
        return failedFuture(new Exception());
    }
}
