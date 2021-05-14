package io.smallrye.faulttolerance.vertx.bulkhead.retry;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.Vertx;

@ApplicationScoped
public class MyService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    @NonBlocking
    @Bulkhead(value = 3, waitingTaskQueue = 3)
    @Retry(maxRetries = 1, delay = 2000, jitter = 0)
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
