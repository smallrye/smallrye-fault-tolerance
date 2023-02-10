package io.smallrye.faulttolerance.reuse.mixed.sync;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);

    @ApplyFaultTolerance("my-fault-tolerance")
    public String hello() {
        if (STRING_COUNTER.incrementAndGet() > 3) {
            return "hello";
        }
        throw new IllegalArgumentException();
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    public int theAnswer() {
        if (INT_COUNTER.incrementAndGet() > 3) {
            return 42;
        }
        throw new IllegalArgumentException();
    }
}
