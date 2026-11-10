package io.smallrye.faulttolerance.retry.computed.delay;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

@Dependent
public class MyComputedDelayDependency {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id = COUNTER.incrementAndGet();

    public int getId() {
        return id;
    }
}
