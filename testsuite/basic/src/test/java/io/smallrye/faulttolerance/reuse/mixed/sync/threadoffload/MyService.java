package io.smallrye.faulttolerance.reuse.mixed.sync.threadoffload;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD_STRING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_INT = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        CURRENT_THREAD_STRING.set(Thread.currentThread());
        return "hello";
    }

    @ApplyGuard("my-fault-tolerance")
    public int theAnswer() {
        CURRENT_THREAD_INT.set(Thread.currentThread());
        return 42;
    }
}
