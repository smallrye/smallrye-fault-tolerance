package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
@Typed(SuperCircuitBreakerService.class)
public class SuperCircuitBreakerService {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "super";
    }
}
