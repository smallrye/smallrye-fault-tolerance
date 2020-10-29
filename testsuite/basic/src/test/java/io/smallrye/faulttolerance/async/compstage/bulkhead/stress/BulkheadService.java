package io.smallrye.faulttolerance.async.compstage.bulkhead.stress;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

public class BulkheadService {
    // must NOT be more than max thread pool size (100), because the test
    // waits for the tasks to actually enter the bulkhead
    static final int BULKHEAD_SIZE = 100;
    static final int BULKHEAD_QUEUE_SIZE = 10000;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Bulkhead(value = BULKHEAD_SIZE, waitingTaskQueue = BULKHEAD_QUEUE_SIZE)
    @Asynchronous
    public CompletionStage<String> hello() {
        return completedFuture("" + counter.getAndIncrement());
    }

    @Bulkhead(value = BULKHEAD_SIZE, waitingTaskQueue = BULKHEAD_QUEUE_SIZE)
    @Asynchronous
    public CompletionStage<String> helloWithWaiting(CountDownLatch startLatch, CountDownLatch endLatch) {
        startLatch.countDown();
        try {
            endLatch.await();
        } catch (InterruptedException ignored) {
        }

        return completedFuture("" + counter.getAndIncrement());
    }

    public void reset() {
        counter.set(0);
    }
}
