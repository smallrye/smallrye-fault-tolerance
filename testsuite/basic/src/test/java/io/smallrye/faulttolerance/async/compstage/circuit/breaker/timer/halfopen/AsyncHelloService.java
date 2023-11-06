package io.smallrye.faulttolerance.async.compstage.circuit.breaker.timer.halfopen;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerName;

@ApplicationScoped
public class AsyncHelloService {
    static final int ROLLING_WINDOW = 10;
    static final int DELAY = 200;

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, delay = DELAY)
    @CircuitBreakerName("hello")
    public CompletionStage<String> hello(boolean fail) {
        if (fail) {
            return failedFuture(new IllegalArgumentException());
        }

        return completedFuture("Hello, world!");
    }
}
