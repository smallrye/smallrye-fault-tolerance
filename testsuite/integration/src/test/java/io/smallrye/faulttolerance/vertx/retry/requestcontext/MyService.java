package io.smallrye.faulttolerance.vertx.retry.requestcontext;

import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.core.util.CompletionStages;

@ApplicationScoped
public class MyService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    private final AtomicInteger counter = new AtomicInteger(0);

    @Inject
    MyRequestScopedService requestScopedService;

    @NonBlocking
    @Retry(maxRetries = 20, delay = 5, delayUnit = ChronoUnit.MILLIS)
    public CompletionStage<String> hello() {
        requestScopedService.call();

        invocationThreads.add(Thread.currentThread().getName());

        int current = counter.incrementAndGet();
        if (current > 10) {
            return CompletableFuture.completedFuture("Hello!");
        }
        return CompletionStages.failedStage(new Exception());
    }
}
