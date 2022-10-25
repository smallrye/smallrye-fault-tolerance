package io.smallrye.faulttolerance.async.additional.asyncnonblocking;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class AsyncNonBlockingHelloService {
    private volatile Thread helloThread;
    private volatile StackTraceElement[] helloStackTrace;

    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        helloThread = Thread.currentThread();
        helloStackTrace = new Throwable().getStackTrace();
        return completedFuture("hello");
    }

    Thread getHelloThread() {
        return helloThread;
    }

    StackTraceElement[] getHelloStackTrace() {
        return helloStackTrace;
    }
}
