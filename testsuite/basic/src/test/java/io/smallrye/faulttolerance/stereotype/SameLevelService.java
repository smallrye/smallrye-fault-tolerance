package io.smallrye.faulttolerance.stereotype;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class SameLevelService {

    private final AtomicInteger callCount = new AtomicInteger(0);

    AtomicInteger getCallCount() {
        return callCount;
    }

    void resetCallCount() {
        callCount.set(0);
    }

    @Retry(maxRetries = 2, delay = 50, delayUnit = ChronoUnit.MILLIS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Bulkhead(value = 10)
    @Fallback(fallbackMethod = "fallback")
    public String work() {
        callCount.incrementAndGet();
        throw new RuntimeException("transient failure");
    }

    String fallback() {
        return "fallback";
    }
}
