package io.smallrye.faulttolerance.reuse.config.guard.invalid;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        COUNTER.incrementAndGet();
        throw new IllegalStateException();
    }

    String fallback() {
        return "hello";
    }
}
