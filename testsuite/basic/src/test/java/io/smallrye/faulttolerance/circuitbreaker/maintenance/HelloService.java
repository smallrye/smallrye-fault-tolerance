package io.smallrye.faulttolerance.circuitbreaker.maintenance;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class HelloService {
    static final String OK = "Hello";
    static final int THRESHOLD = 5;
    static final int DELAY = 500;

    @CircuitBreaker(requestVolumeThreshold = THRESHOLD, delay = DELAY)
    @CircuitBreakerName("hello")
    public String hello(Exception exception) throws Exception {
        if (exception != null) {
            throw exception;
        }

        return OK;
    }
}
