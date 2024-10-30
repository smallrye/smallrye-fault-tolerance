package io.smallrye.faulttolerance.async.compstage.retry.beforeretry;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;

@ApplicationScoped
public class AsyncHelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);
    static final AtomicInteger BEFORE_RETRY_COUNTER = new AtomicInteger(0);

    @Asynchronous
    @Retry(maxRetries = 2)
    @BeforeRetry(methodName = "beforeRetry")
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        COUNTER.incrementAndGet();
        return failedFuture(new IOException("Simulated IO error"));
    }

    private void beforeRetry() {
        BEFORE_RETRY_COUNTER.incrementAndGet();
    }

    private CompletionStage<String> fallback() {
        return completedFuture("Fallback");
    }
}
