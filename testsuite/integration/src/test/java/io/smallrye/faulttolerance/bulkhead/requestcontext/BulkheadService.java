package io.smallrye.faulttolerance.bulkhead.requestcontext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class BulkheadService {
    static final List<String> observedCorrelationIds = new CopyOnWriteArrayList<>();

    @Inject
    CorrelationId correlationId;

    @AsynchronousNonBlocking
    @Bulkhead(value = 1, waitingTaskQueue = 5)
    public CompletionStage<String> call(CompletableFuture<String> completion) {
        String id = correlationId.get();
        observedCorrelationIds.add(id);
        return completion.thenApply(ignored -> id);
    }
}
