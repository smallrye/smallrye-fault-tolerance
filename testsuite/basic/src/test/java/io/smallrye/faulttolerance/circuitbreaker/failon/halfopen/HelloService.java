package io.smallrye.faulttolerance.circuitbreaker.failon.halfopen;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class HelloService {
    static final int ROLLING_WINDOW = 4;
    static final int DELAY = 200;

    private final AtomicInteger counter = new AtomicInteger(0);

    @CircuitBreaker(requestVolumeThreshold = ROLLING_WINDOW, failOn = IllegalArgumentException.class, delay = DELAY)
    public String hello(Exception e) throws Exception {
        counter.incrementAndGet();

        if (e == null) {
            return "Hello, world!";
        }
        throw e;
    }

    AtomicInteger getCounter() {
        return counter;
    }
}
