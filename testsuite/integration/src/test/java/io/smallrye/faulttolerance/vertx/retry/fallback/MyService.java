package io.smallrye.faulttolerance.vertx.retry.fallback;

import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.core.util.CompletionStages;

@ApplicationScoped
public class MyService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    @NonBlocking
    @Retry(maxRetries = 10, delay = 5, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        invocationThreads.add(Thread.currentThread().getName());
        return CompletionStages.failedStage(new Exception());
    }

    public CompletionStage<String> fallback() {
        invocationThreads.add(Thread.currentThread().getName());
        return CompletableFuture.completedFuture("Hello fallback!");
    }
}
