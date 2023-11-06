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

import io.smallrye.common.annotation.NonBlocking;

@ApplicationScoped
public class MyService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    @NonBlocking
    @Retry(maxRetries = 10, delay = 5, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        invocationThreads.add(Thread.currentThread().getName());
        return failedFuture(new Exception());
    }

    public CompletionStage<String> fallback() {
        invocationThreads.add(Thread.currentThread().getName());
        return completedFuture("Hello fallback!");
    }
}
