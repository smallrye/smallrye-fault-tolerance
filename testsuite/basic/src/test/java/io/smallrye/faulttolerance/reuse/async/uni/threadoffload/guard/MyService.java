package io.smallrye.faulttolerance.reuse.async.uni.threadoffload.guard;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_NONBLOCKING = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public Uni<String> hello() {
        CURRENT_THREAD.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public Uni<String> helloBlocking() {
        CURRENT_THREAD_BLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public Uni<String> helloNonBlocking() {
        CURRENT_THREAD_NONBLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }
}
