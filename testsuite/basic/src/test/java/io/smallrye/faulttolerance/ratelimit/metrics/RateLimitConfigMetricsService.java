package io.smallrye.faulttolerance.ratelimit.metrics;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.RateLimit;

@ApplicationScoped
public class RateLimitConfigMetricsService {
    @RateLimit(5)
    public void plainMethod() {
    }

    @RateLimit(value = 10, window = 2, windowUnit = ChronoUnit.SECONDS, minSpacing = 500, minSpacingUnit = ChronoUnit.MILLIS)
    public void explicitUnitsMethod() {
    }

    @RateLimit(7)
    public void overriddenMethod() {
    }
}
