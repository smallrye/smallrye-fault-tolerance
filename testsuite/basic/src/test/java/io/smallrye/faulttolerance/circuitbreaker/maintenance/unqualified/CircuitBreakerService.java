package io.smallrye.faulttolerance.circuitbreaker.maintenance.unqualified;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class CircuitBreakerService {
    @CircuitBreaker
    @CircuitBreakerName("hello")
    public String hello() {
        return "hello";
    }

    @ApplicationScoped
    public static class Watcher {
        @Inject
        private CircuitBreakerMaintenance cb;
    }
}
