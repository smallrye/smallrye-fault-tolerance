package io.smallrye.faulttolerance.retry.when.result;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.retry.when.IsNull;

@ApplicationScoped
public class RetryWhenResultDoesNotMatchService {
    private final AtomicInteger attempts = new AtomicInteger();

    @Retry
    @RetryWhen(result = IsNull.class)
    public String hello() {
        attempts.incrementAndGet();
        return "hello";
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }
}
