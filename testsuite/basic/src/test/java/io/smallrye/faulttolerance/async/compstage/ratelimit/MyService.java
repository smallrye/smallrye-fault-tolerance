package io.smallrye.faulttolerance.async.compstage.ratelimit;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RateLimitType;

@ApplicationScoped
public class MyService {
    static final int RATE_LIMIT = 50;

    @Asynchronous
    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.FIXED)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> fixedWindowNoSpacing() {
        return completedFuture("hello");
    }

    @Asynchronous
    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.FIXED, minSpacing = 10, minSpacingUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> fixedWindowWithSpacing() {
        return completedFuture("hello");
    }

    @Asynchronous
    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.ROLLING)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> rollingWindowNoSpacing() {
        return completedFuture("hello");
    }

    @Asynchronous
    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.ROLLING, minSpacing = 10, minSpacingUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> rollingWindowWithSpacing() {
        return completedFuture("hello");
    }

    private CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
