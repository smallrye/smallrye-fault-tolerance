package io.smallrye.faulttolerance.bulkhead.capacity.overflow;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

@ApplicationScoped
public class MyService {
    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = Integer.MAX_VALUE)
    public CompletionStage<String> hello() {
        return completedFuture("hello");
    }
}
