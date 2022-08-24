package io.smallrye.faulttolerance.async.additional.asyncnonblocking.retry;

import static io.smallrye.faulttolerance.core.util.CompletionStages.failedFuture;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
@Retry(maxRetries = 3, delay = 0, jitter = 0)
public class RetryAsyncNonBlockingHelloService {
    private final List<Thread> helloThreads = new CopyOnWriteArrayList<>();
    private final List<StackTraceElement[]> helloStackTraces = new CopyOnWriteArrayList<>();

    private final AtomicInteger invocationCounter = new AtomicInteger();

    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        invocationCounter.incrementAndGet();
        helloThreads.add(Thread.currentThread());
        helloStackTraces.add(new Throwable().getStackTrace());
        return failedFuture(new RuntimeException());
    }

    List<Thread> getHelloThreads() {
        return helloThreads;
    }

    List<StackTraceElement[]> getHelloStackTraces() {
        return helloStackTraces;
    }

    AtomicInteger getInvocationCounter() {
        return invocationCounter;
    }
}
