package io.smallrye.faulttolerance.async.additional.blocking.unguarded;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.common.annotation.Blocking;

@ApplicationScoped
public class UnguardedBlockingHelloService {
    private volatile Thread helloThread;
    private volatile StackTraceElement[] helloStackTrace;

    @Blocking
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
