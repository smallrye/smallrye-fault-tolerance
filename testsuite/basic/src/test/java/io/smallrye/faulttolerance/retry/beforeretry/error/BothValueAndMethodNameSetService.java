package io.smallrye.faulttolerance.retry.beforeretry.error;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.BeforeRetryHandler;

@Dependent
public class BothValueAndMethodNameSetService {
    @Retry
    @BeforeRetry(value = MyHandler.class, methodName = "beforeRetry")
    public void hello() {
        throw new IllegalArgumentException();
    }

    void beforeRetry() {
    }

    static class MyHandler implements BeforeRetryHandler {
        @Override
        public void handle(ExecutionContext context) {
        }
    }
}
