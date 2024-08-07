package io.smallrye.faulttolerance.retry.beforeretry.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;

@Dependent
public class NoBeforeRetryMethodFoundService {
    @Retry
    @BeforeRetry(methodName = "beforeRetry")
    public void hello() {
        throw new IllegalArgumentException();
    }

    public int beforeRetry(int param) {
        return 0;
    }
}
