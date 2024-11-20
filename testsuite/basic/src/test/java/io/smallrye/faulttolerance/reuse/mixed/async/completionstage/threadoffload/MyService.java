package io.smallrye.faulttolerance.reuse.mixed.async.completionstage.threadoffload;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class MyService {
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_HELLO_NONBLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER_BLOCKING = new AtomicReference<>();
    static final AtomicReference<Thread> CURRENT_THREAD_THEANSWER_NONBLOCKING = new AtomicReference<>();

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<String> hello() {
        CURRENT_THREAD_HELLO.set(Thread.currentThread());
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public CompletionStage<String> helloBlocking() {
        CURRENT_THREAD_HELLO_BLOCKING.set(Thread.currentThread());
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public CompletionStage<String> helloNonBlocking() {
        CURRENT_THREAD_HELLO_NONBLOCKING.set(Thread.currentThread());
        return completedFuture("hello");
    }

    @ApplyGuard("my-fault-tolerance")
    public CompletionStage<Integer> theAnswer() {
        CURRENT_THREAD_THEANSWER.set(Thread.currentThread());
        return completedFuture(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @Asynchronous
    public CompletionStage<Integer> theAnswerBlocking() {
        CURRENT_THREAD_THEANSWER_BLOCKING.set(Thread.currentThread());
        return completedFuture(42);
    }

    @ApplyGuard("my-fault-tolerance")
    @AsynchronousNonBlocking
    public CompletionStage<Integer> theAnswerNonBlocking() {
        CURRENT_THREAD_THEANSWER_NONBLOCKING.set(Thread.currentThread());
        return completedFuture(42);
    }
}
