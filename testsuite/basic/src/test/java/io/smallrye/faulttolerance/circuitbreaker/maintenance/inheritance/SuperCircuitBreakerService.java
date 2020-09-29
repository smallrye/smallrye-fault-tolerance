package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class SuperCircuitBreakerService {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "super";
    }
}
