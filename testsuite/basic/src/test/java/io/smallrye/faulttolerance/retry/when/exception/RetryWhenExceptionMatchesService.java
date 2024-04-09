package io.smallrye.faulttolerance.retry.when.exception;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.retry.when.IsIllegalArgumentException;

@ApplicationScoped
public class RetryWhenExceptionMatchesService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry
    @RetryWhen(exception = IsIllegalArgumentException.class)
    public void hello() {
        attempts.incrementAndGet();
        throw new IllegalArgumentException();
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
