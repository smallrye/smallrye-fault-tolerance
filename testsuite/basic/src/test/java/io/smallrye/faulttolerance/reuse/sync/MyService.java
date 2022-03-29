package io.smallrye.faulttolerance.reuse.sync;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyFaultTolerance("my-fault-tolerance")
    public String hello() {
        COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }
}
