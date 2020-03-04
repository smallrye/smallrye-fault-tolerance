package io.smallrye.faulttolerance.async.compstage.retry.circuit.breaker.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

@ApplicationScoped
public class AsyncHelloService {
    @Asynchronous
    @Retry(retryOn = IOException.class)
    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 5, successThreshold = 3, delay = 2, delayUnit = ChronoUnit.SECONDS, failureRatio = 0.75)
    @Fallback(fallbackMethod = "fallback", applyOn = { IOException.class, CircuitBreakerOpenException.class })
    public CompletionStage<String> hello(int counter) throws IOException {
        // 3/4 requests trigger IOException
        if (counter % 4 != 0) {
            throw new IOException("Simulated IOException");
        }

        return completedFuture("Hello" + counter);
    }

    @Asynchronous
    @Retry(retryOn = IOException.class)
    @CircuitBreaker(failOn = IOException.class, requestVolumeThreshold = 5, successThreshold = 3, delay = 2, delayUnit = ChronoUnit.SECONDS, failureRatio = 0.75)
    @Fallback(fallbackMethod = "fallback", applyOn = { IOException.class, CircuitBreakerOpenException.class })
    public CompletionStage<String> helloFailAsync(int counter) {
        // 3/4 requests trigger IOException
        if (counter % 4 != 0) {
            CompletableFuture<String> result = new CompletableFuture<>();
            result.completeExceptionally(new IOException("Simulated IOException"));
            return result;
        }

        return completedFuture("Hello" + counter);
    }

    public CompletionStage<String> fallback(int counter) {
        return completedFuture("Fallback" + counter);
    }
}
