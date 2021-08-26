package io.smallrye.faulttolerance.before.retry;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetryAnnotation;

@Dependent
public class BeforeRetryTestBean {

    private final AtomicInteger attempt = new AtomicInteger();

    private final AtomicInteger beforeRetryRuns = new AtomicInteger();

    @Retry(maxRetries = 2)
    @BeforeRetryAnnotation(beforeRetryMethod = "beforeRetry")
    public String call() {
        if (attempt.getAndIncrement() < 1) {
            throw new IllegalStateException();
        }
        return "call " + attempt.get();
    }

    public void beforeRetry() {
        beforeRetryRuns.incrementAndGet();
    }

    public void reset() {
        attempt.set(0);
        beforeRetryRuns.set(0);
    }

    int getBeforeRetryRuns() {
        return beforeRetryRuns.get();
    }
}
