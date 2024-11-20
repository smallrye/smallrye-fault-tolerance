package io.smallrye.faulttolerance.reuse.mixed.async.uni.threadoffload;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO_NONBLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER_NONBLOCKING = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public Uni<String> hello() {
        CURRENT_THREAD_HELLO.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public Uni<String> helloBlocking() {
        CURRENT_THREAD_HELLO_BLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public Uni<String> helloNonBlocking() {
        CURRENT_THREAD_HELLO_NONBLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    public Uni<Integer> theAnswer() {
        CURRENT_THREAD_THEANSWER.set(Thread.currentThread());
        return Uni.createFrom().item(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public Uni<Integer> theAnswerBlocking() {
        CURRENT_THREAD_THEANSWER_BLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public Uni<Integer> theAnswerNonBlocking() {
        CURRENT_THREAD_THEANSWER_NONBLOCKING.set(Thread.currentThread());
        return Uni.createFrom().item(42);
    }
}
