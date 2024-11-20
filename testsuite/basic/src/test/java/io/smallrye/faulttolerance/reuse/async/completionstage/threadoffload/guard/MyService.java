package io.smallrye.faulttolerance.reuse.async.completionstage.threadoffload.guard;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_NONBLOCKING = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> hello() {
        CURRENT_THREAD.set(Thread.currentThread());
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public CompletionStage<String> helloBlocking() {
        CURRENT_THREAD_BLOCKING.set(Thread.currentThread());
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public CompletionStage<String> helloNonBlocking() {
        CURRENT_THREAD_NONBLOCKING.set(Thread.currentThread());
        return completedFuture("hello");
    }
}
