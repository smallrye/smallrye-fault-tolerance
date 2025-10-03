package io.smallrye.faulttolerance.vertx.future;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.vertx.core.Future;

@ApplicationScoped
public class HelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @Asynchronous
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Future<String> helloAsynchronous() {
        COUNTER.incrementAndGet();
        return Future.failedFuture(new IllegalArgumentException());
    }

    @AsynchronousNonBlocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Future<String> helloAsynchronousNonBlocking() {
        COUNTER.incrementAndGet();
        return Future.failedFuture(new IllegalArgumentException());
    }

    public Future<String> fallback() {
        return Future.succeededFuture("hello");
    }
}
