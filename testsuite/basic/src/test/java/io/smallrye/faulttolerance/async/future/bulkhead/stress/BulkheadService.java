package io.smallrye.faulttolerance.async.future.bulkhead.stress;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

public class BulkheadService {
    // must be a fraction of max thread pool size, not more than 1/5
    // (this is because thread usage in thread pool style bulkheads isn't very efficient yet)
    static final int BULKHEAD_SIZE = 10;
    static final int BULKHEAD_QUEUE_SIZE = 1000;

    private AtomicInteger counter = new AtomicInteger(0);

    @Bulkhead(value = BULKHEAD_SIZE, waitingTaskQueue = BULKHEAD_QUEUE_SIZE)
    @Asynchronous
    public Future<String> hello() {
        return completedFuture("" + counter.getAndIncrement());
    }
}
