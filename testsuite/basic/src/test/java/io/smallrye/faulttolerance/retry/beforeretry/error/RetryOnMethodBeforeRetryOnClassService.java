package io.smallrye.faulttolerance.retry.beforeretry.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;

@Dependent
@BeforeRetry(methodName = "beforeRetry")
public class RetryOnMethodBeforeRetryOnClassService {
    @Retry
    public void hello() {
        throw new IllegalArgumentException();
    }

    void beforeRetry() {
    }
}
