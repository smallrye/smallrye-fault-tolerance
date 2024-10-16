package io.smallrye.faulttolerance.reuse.async.uni.metrics;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyFaultTolerance("my-fault-tolerance")
    public Uni<String> first() {
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    public Uni<String> second() {
        return Uni.createFrom().failure(new IllegalStateException());
    }
}
