package io.smallrye.faulttolerance.async.compstage.exception;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class AsyncHelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @Asynchronous
    @Retry(maxRetries = 4, retryOn = { HelloException.class })
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> hello() {
        COUNTER.incrementAndGet();
        return completedFuture("hello").handle((value, exception) -> {
            throw new HelloException();
        });
    }

    public CompletionStage<String> fallback() {
        return completedFuture("hello fallback");
    }
}
