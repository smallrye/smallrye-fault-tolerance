package io.smallrye.faulttolerance.reuse.async.uni.fallback.typedguard;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> hello() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    Uni<String> fallback() {
        return Uni.createFrom().item("better fallback");
    }
}
