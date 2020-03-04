package io.smallrye.faulttolerance.async.compstage.bulkhead.timeout.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class AsyncHelloService {
    @Asynchronous
    @Bulkhead(value = 15, waitingTaskQueue = 15)
    @Timeout(value = 1, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "fallback")
    public CompletionStage<String> bulkheadTimeout(boolean fail) throws InterruptedException {
        if (fail) {
            Thread.sleep(2000);
        }
        return completedFuture("Hello from @Bulkhead @Timeout method");
    }

    public CompletionStage<String> fallback(boolean fail) {
        return completedFuture("Fallback Hello");
    }
}
