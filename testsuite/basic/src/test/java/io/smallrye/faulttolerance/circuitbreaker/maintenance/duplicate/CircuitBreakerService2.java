package io.smallrye.faulttolerance.circuitbreaker.maintenance.duplicate;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class CircuitBreakerService2 {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "2";
    }
}
