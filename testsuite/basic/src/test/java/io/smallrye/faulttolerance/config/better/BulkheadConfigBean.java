package io.smallrye.faulttolerance.config.better;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import io.smallrye.faulttolerance.core.util.barrier.Barrier;

@ApplicationScoped
public class BulkheadConfigBean {
    @Bulkhead(value = 5)
    public void value(Barrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw sneakyThrow(e);
        }
    }

    @Bulkhead(value = 1, waitingTaskQueue = 5)
    @Asynchronous
    public Future<Void> waitingTaskQueue(Barrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw sneakyThrow(e);
        }
        return completedFuture(null);
    }
}
