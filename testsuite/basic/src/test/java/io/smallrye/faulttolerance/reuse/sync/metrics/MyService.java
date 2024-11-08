package io.smallrye.faulttolerance.reuse.sync.metrics;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String first() {
        throw new IllegalArgumentException();
    }

    @ApplyGuard("my-fault-tolerance")
    public String second() {
        throw new IllegalStateException();
    }
}
