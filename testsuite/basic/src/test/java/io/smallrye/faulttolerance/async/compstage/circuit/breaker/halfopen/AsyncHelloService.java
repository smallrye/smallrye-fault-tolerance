package io.smallrye.faulttolerance.async.compstage.circuit.breaker.halfopen;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.core.util.party.Party;

@ApplicationScoped
public class AsyncHelloService {
    static final int ROLLING_WINDOW = 10;
    static final int DELAY = 200;
    static final int PROBE_ATTEMPTS = 5;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Asynchronous
    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, delay = DELAY, successThreshold = PROBE_ATTEMPTS)
    public CompletionStage<String> hello(boolean fail, Party.Participant participant) throws InterruptedException {
        counter.incrementAndGet();

        if (fail) {
            return failedStage(new IllegalArgumentException());
        }

        if (participant != null) {
            participant.attend();
        }

        return completedStage("Hello, world!");
    }

    AtomicInteger getCounter() {
        return counter;
    }
}
