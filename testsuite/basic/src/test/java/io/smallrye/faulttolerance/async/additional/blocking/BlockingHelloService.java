package io.smallrye.faulttolerance.async.additional.blocking;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.common.annotation.Blocking;

@ApplicationScoped
public class BlockingHelloService {
    private volatile Thread helloThread;
    private volatile StackTraceElement[] helloStackTrace;

    @Blocking
    @Asynchronous
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
