package io.smallrye.faulttolerance.circuitbreaker.halfopen;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.core.util.party.Party;

@ApplicationScoped
public class HelloService {
    static final int ROLLING_WINDOW = 10;
    static final int DELAY = 200;
    static final int PROBE_ATTEMPTS = 5;

    private final AtomicInteger counter = new AtomicInteger(0);

    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, delay = DELAY, successThreshold = PROBE_ATTEMPTS)
    public String hello(boolean fail, Party.Participant participant) throws InterruptedException {
        counter.incrementAndGet();

        if (fail) {
            throw new IllegalArgumentException();
        }

        if (participant != null) {
            participant.attend();
        }

        return "Hello, world!";
    }

    AtomicInteger getCounter() {
        return counter;
    }
}
