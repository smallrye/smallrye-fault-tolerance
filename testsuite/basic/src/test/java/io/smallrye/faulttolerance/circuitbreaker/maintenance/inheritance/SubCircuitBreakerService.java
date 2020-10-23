package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
@CircuitBreaker
@Typed(SubCircuitBreakerService.class)
public class SubCircuitBreakerService extends SuperCircuitBreakerService {
    @Override
    public String hello() {
        return "sub";
    }
}
