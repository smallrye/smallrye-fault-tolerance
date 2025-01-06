package io.smallrye.faulttolerance.bulkhead.capacity.overflow;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class MyService {
    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = Integer.MAX_VALUE)
    public CompletionStage<String> hello() {
        return completedFuture("hello");
    }
}
