package io.smallrye.faulttolerance.ratelimit;

import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RateLimitType;

@ApplicationScoped
public class MyService {
    static final int RATE_LIMIT = 50;

    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.FIXED)
    @Fallback(fallbackMethod = "fallback")
    public String fixedWindowNoSpacing() {
        return "hello";
    }

    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.FIXED, minSpacing = 10, minSpacingUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public String fixedWindowWithSpacing() {
        return "hello";
    }

    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.ROLLING)
    @Fallback(fallbackMethod = "fallback")
    public String rollingWindowNoSpacing() {
        return "hello";
    }

    @RateLimit(value = RATE_LIMIT, window = 1, windowUnit = ChronoUnit.MINUTES, type = RateLimitType.ROLLING, minSpacing = 10, minSpacingUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public String rollingWindowWithSpacing() {
        return "hello";
    }

    private String fallback() {
        return "fallback";
    }
}
