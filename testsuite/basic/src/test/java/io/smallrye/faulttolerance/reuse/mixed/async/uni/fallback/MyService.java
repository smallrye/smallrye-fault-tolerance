package io.smallrye.faulttolerance.reuse.mixed.async.uni.fallback;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "helloFallback")
    public Uni<String> hello() {
        STRING_COUNTER.incrementAndGet();
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    Uni<String> helloFallback() {
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "theAnswerFallback")
    public Uni<Integer> theAnswer() {
        INT_COUNTER.incrementAndGet();
        return Uni.createFrom().failure(new IllegalArgumentException());
    }

    Uni<Integer> theAnswerFallback() {
        return Uni.createFrom().item(42);
    }
}
