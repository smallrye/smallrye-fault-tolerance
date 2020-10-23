package io.smallrye.faulttolerance.async.future.bulkhead.stress;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

@Dependent
public class BulkheadService {
    // can be more than max thread pool size (100), because the test
    // doesn't wait for the tasks to actually enter the bulkhead
    static final int BULKHEAD_SIZE = 200;
    static final int BULKHEAD_QUEUE_SIZE = 20000;

    private AtomicInteger counter = new AtomicInteger(0);

    @Bulkhead(value = BULKHEAD_SIZE, waitingTaskQueue = BULKHEAD_QUEUE_SIZE)
    @Asynchronous
    public Future<String> hello() {
        return completedFuture("" + counter.getAndIncrement());
    }

    public void reset() {
        counter.set(0);
    }
}
