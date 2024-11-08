package io.smallrye.faulttolerance.reuse.sync;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }
}
