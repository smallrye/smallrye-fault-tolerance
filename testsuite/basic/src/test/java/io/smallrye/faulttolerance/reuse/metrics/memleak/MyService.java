package io.smallrye.faulttolerance.reuse.metrics.memleak;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        throw new IllegalArgumentException();
    }
}
