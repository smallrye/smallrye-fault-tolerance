package io.smallrye.faulttolerance.reuse.async.uni;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    public Uni<String> hello() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(new IllegalArgumentException());
    }
}
