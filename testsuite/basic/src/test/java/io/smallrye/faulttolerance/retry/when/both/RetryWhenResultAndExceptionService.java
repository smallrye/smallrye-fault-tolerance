package io.smallrye.faulttolerance.retry.when.both;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.retry.when.IsIllegalArgumentException;
import io.smallrye.faulttolerance.retry.when.IsNull;

@ApplicationScoped
public class RetryWhenResultAndExceptionService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry
    @RetryWhen(result = IsNull.class, exception = IsIllegalArgumentException.class)
    public String hello() {
        int current = attempts.incrementAndGet();
        if (current == 1) {
            return null;
        } else if (current == 2) {
            throw new IllegalArgumentException();
        } else {
            return "hello";
        }
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
