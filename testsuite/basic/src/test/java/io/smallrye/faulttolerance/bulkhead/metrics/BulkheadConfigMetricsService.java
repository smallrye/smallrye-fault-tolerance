package io.smallrye.faulttolerance.bulkhead.metrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class BulkheadConfigMetricsService {
    @Bulkhead(value = 3, waitingTaskQueue = 4)
    public void syncMethod() {
    }

    @AsynchronousNonBlocking
    @Bulkhead(value = 5, waitingTaskQueue = 6)
    public CompletionStage<Void> asyncMethod() {
        return CompletableFuture.completedFuture(null);
    }

    @Bulkhead(value = 7)
    public void overriddenMethod() {
    }
}
