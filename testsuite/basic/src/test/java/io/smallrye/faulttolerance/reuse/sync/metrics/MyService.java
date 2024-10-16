package io.smallrye.faulttolerance.reuse.sync.metrics;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class MyService {
    @ApplyFaultTolerance("my-fault-tolerance")
    public String first() {
        throw new IllegalArgumentException();
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    public String second() {
        throw new IllegalStateException();
    }
}
