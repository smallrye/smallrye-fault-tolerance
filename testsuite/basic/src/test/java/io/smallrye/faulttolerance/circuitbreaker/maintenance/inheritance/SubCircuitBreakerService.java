package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
@CircuitBreaker
public class SubCircuitBreakerService extends SuperCircuitBreakerService {
    @Override
    public String hello() {
        return "sub";
    }
}
