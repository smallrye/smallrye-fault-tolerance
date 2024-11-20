package io.smallrye.faulttolerance.reuse.sync.threadoffload.typedguard;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyGuard;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public String hello() {
        CURRENT_THREAD.set(Thread.currentThread());
        return "hello";
    }
}
