package io.smallrye.faulttolerance.reuse.async.uni.metrics;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @ApplyGuard("my-fault-tolerance")
    public Uni<String> first() {
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<String> second() {
        return Uni.createFrom().failure(new IllegalStateException());
    }
}
