package io.smallrye.faulttolerance.bulkhead.requestcontext;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;

/**
 * An invocation that had to wait in the bulkhead queue runs on the thread that completed some
 * previous invocation. It must still see its own request context, not the request context
 * of that previous invocation.
 */
@FaultToleranceIntegrationTest
public class BulkheadRequestContextTest {
    @Inject
    RequestContextController rcc;

    @Inject
    CorrelationId correlationId;

    @Inject
    BulkheadService service;

    @BeforeEach
    public void setUp() {
        BulkheadService.observedCorrelationIds.clear();
    }

    @Test
    public void queuedInvocationSeesItsOwnRequestContext() throws Exception {
        CompletableFuture<String> completionA = new CompletableFuture<>();
        CompletableFuture<String> completionB = completedFuture("completionB");

        // the 1st request occupies the only bulkhead permit until `completionA` completes
        CompletableFuture<String> requestA = inOwnRequestContext("correlation-A", () -> service.call(completionA));
        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A");

        // the 2nd request therefore has to wait in the bulkhead queue
        CompletableFuture<String> requestB = inOwnRequestContext("correlation-B", () -> service.call(completionB));
        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A");

        // the 1st invocation completes on a thread that has no request context
        // the queued 2nd invocation runs on that same thread
        completionA.complete("completionA");

        assertThat(requestA.get(10, TimeUnit.SECONDS)).isEqualTo("correlation-A");
        assertThat(requestB.get(10, TimeUnit.SECONDS)).isEqualTo("correlation-B");

        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A", "correlation-B");
    }

    @Test
    public void queuedInvocationDoesNotInheritRequestContextOfCompletingThread() throws Exception {
        CompletableFuture<String> completionA = new CompletableFuture<>();
        CompletableFuture<String> completionB = completedFuture("completionB");

        // the 1st request occupies the only bulkhead permit until `completionA` completes
        CompletableFuture<String> requestA = inOwnRequestContext("correlation-A", () -> service.call(completionA));

        // the 2nd request therefore has to wait in the bulkhead queue
        CompletableFuture<String> requestB = inOwnRequestContext("correlation-B", () -> service.call(completionB));
        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A");

        // the 1st invocation completes on a thread that is itself serving an unrelated 3rd request
        // the queued 2nd invocation runs on that same thread, but must not see the unrelated context
        inOwnRequestContext("correlation-C", () -> completionA.complete("completionA"));

        assertThat(requestA.get(10, TimeUnit.SECONDS)).isEqualTo("correlation-A");
        assertThat(requestB.get(10, TimeUnit.SECONDS)).isEqualTo("correlation-B");

        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A", "correlation-B");
    }

    @Test
    public void sequentialInvocationsSeeTheirOwnRequestContext() throws Exception {
        for (String correlation : new String[] { "correlation-A", "correlation-B" }) {
            CompletableFuture<String> request = inOwnRequestContext(correlation, () -> service.call(completedFuture("result")));
            assertThat(request.get(10, TimeUnit.SECONDS)).isEqualTo(correlation);
        }

        assertThat(BulkheadService.observedCorrelationIds).containsExactly("correlation-A", "correlation-B");
    }

    private void inOwnRequestContext(String correlationId, Runnable action) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<String> correlationAtEnd = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            boolean activated = rcc.activate();
            try {
                this.correlationId.set(correlationId);
                action.run();
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                correlationAtEnd.set(this.correlationId.get());
                if (activated) {
                    rcc.deactivate();
                }
            }
        }, "request-" + correlationId);
        thread.start();
        thread.join(10_000);
        assertThat(thread.isAlive()).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(correlationAtEnd.get()).isEqualTo(correlationId);
    }

    private CompletableFuture<String> inOwnRequestContext(String correlationId, Supplier<CompletionStage<String>> invocation)
            throws InterruptedException {
        CompletableFuture<String> result = new CompletableFuture<>();
        CountDownLatch submitted = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            boolean activated = rcc.activate();
            try {
                this.correlationId.set(correlationId);
                CompletionStage<String> running;
                try {
                    running = invocation.get();
                } finally {
                    submitted.countDown();
                }
                running.whenComplete((value, error) -> {
                    if (error == null) {
                        result.complete(value);
                    } else {
                        result.completeExceptionally(error);
                    }
                });
                // keep the request context active until the invocation completes
                result.exceptionally(error -> null).join();
            } catch (Throwable e) {
                result.completeExceptionally(e);
            } finally {
                if (activated) {
                    rcc.deactivate();
                }
            }
        }, "request-" + correlationId);
        thread.setDaemon(true);
        thread.start();
        assertThat(submitted.await(10, TimeUnit.SECONDS)).isTrue();
        return result;
    }
}
