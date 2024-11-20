package io.smallrye.faulttolerance.reuse.mixed.sync.fallback;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicInteger STRING_COUNTER = new AtomicInteger(0);
    static final AtomicInteger INT_COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "helloFallback")
    public String hello() {
        STRING_COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }

    String helloFallback() {
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "theAnswerFallback")
    public int theAnswer() {
        INT_COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }

    int theAnswerFallback() {
        return 42;
    }
}
