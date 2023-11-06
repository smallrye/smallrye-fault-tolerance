package io.smallrye.faulttolerance.async.compstage.circuit.breaker.failon.halfopen;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@ApplicationScoped
public class AsyncHelloService {
    static final int ROLLING_WINDOW = 4;
    static final int DELAY = 200;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, failOn = IllegalArgumentException.class, delay = DELAY)
    public CompletionStage<String> hello(Exception e) {
        counter.incrementAndGet();

        if (e == null) {
            return completedFuture("Hello, world!");
        }
        return failedFuture(e);
    }

    AtomicInteger getCounter() {
        return counter;
    }
}
