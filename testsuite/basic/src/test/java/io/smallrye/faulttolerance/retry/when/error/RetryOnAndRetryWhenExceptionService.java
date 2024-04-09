package io.smallrye.faulttolerance.retry.when.error;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.retry.when.IsIllegalArgumentException;

@ApplicationScoped
public class RetryOnAndRetryWhenExceptionService {
    @Retry(retryOn = IllegalStateException.class)
    @RetryWhen(exception = IsIllegalArgumentException.class)
    public void hello() {
    }
}
