package io.smallrye.faulttolerance.circuitbreaker.maintenance.duplicate;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class CircuitBreakerService1 {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "1";
    }
}
