package io.smallrye.faulttolerance.circuitbreaker.timer.halfopen;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class HelloService {
    static final int ROLLING_WINDOW = 10;
    static final int DELAY = 200;

    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, delay = DELAY)
    @CircuitBreakerName("hello")
    public String hello(boolean fail) throws InterruptedException {
        if (fail) {
            throw new IllegalArgumentException();
        }

        return "Hello, world!";
    }
}
