package io.smallrye.faulttolerance.async.additional.asyncnonblocking.priority;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
@Asynchronous
public class AsyncNonBlockingPriorityHelloService {
    // `static` because getters for instance fields would be affected by class-level `@Asynchronous`
    // (and accessing instance fields directly wouldn't work, because this bean is normal-scoped)
    static volatile Thread helloThread;
    static volatile StackTraceElement[] helloStackTrace;

    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        helloThread = Thread.currentThread();
        helloStackTrace = new Throwable().getStackTrace();
        return completedFuture("hello");
    }
}
