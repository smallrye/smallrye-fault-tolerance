package io.smallrye.faulttolerance.circuitbreaker.metrics;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
public class CircuitBreakerConfigMetricsService {
    @CircuitBreaker
    public void plainMethod() {
    }

    @CircuitBreaker(delay = 2, delayUnit = ChronoUnit.SECONDS, requestVolumeThreshold = 10, failureRatio = 0.25, successThreshold = 3)
    public void explicitUnitsMethod() {
    }

    @CircuitBreaker(delay = 1000)
    public void overriddenMethod() {
    }
}
