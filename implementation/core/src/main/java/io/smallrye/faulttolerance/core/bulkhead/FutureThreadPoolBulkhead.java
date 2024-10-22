package io.smallrye.faulttolerance.core.bulkhead;

import java.util.concurrent.Future;

import io.smallrye.faulttolerance.core.FaultToleranceStrategy;

/**
 * Thread pool style bulkhead for {@code Future} asynchronous executions.
 * <p>
 * Since this bulkhead is already executed on an extra thread, we don't have to
 * submit tasks to another executor or use an actual queue. We just limit
 * the task execution by semaphores. This kinda defeats the purpose of bulkheads,
 * but is the easiest way to implement them for {@code Future}. We do it properly
 * for {@code CompletionStage}, which is much more useful anyway.
 */
public class FutureThreadPoolBulkhead<V> extends SemaphoreThreadPoolBulkhead<Future<V>> {
    public FutureThreadPoolBulkhead(FaultToleranceStrategy<Future<V>> delegate, String description, int size, int queueSize) {
        super(delegate, description, size, queueSize);
    }
}
