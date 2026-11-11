package io.smallrye.faulttolerance.metrics.sameid;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.faulttolerance.api.RateLimit;

@ApplicationScoped
public class MyService {
    static final AtomicInteger FALLBACK_COUNTER = new AtomicInteger();
    static final AtomicInteger RETRY_COUNTER = new AtomicInteger();
    static final AtomicInteger CIRCUIT_BREAKER_COUNTER = new AtomicInteger();
    static final AtomicInteger RATE_LIMIT_COUNTER = new AtomicInteger();
    static final AtomicInteger TIMEOUT_COUNTER = new AtomicInteger();
    static final AtomicInteger BULKHEAD_COUNTER = new AtomicInteger();

    @Fallback(fallbackMethod = "fallback")
    public String hello() {
        FALLBACK_COUNTER.incrementAndGet();
        throw new IllegalArgumentException();
    }

    private String fallback() {
        return "hello";
    }

    @Retry(jitter = 0)
    public String hello(int ignored) {
        int count = RETRY_COUNTER.incrementAndGet();
        if (count < 3) {
            throw new IllegalArgumentException();
        }
        return "hello";
    }

    @CircuitBreaker
    public String hello(String ignored) {
        CIRCUIT_BREAKER_COUNTER.incrementAndGet();
        return "hello";
    }

    @RateLimit
    public String hello(List<String> ignored) {
        RATE_LIMIT_COUNTER.incrementAndGet();
        return "hello";
    }

    @Timeout
    public String hello(Set<String> ignored) {
        TIMEOUT_COUNTER.incrementAndGet();
        return "hello";
    }

    @Bulkhead
    public String hello(Map<String, String> ignored) {
        BULKHEAD_COUNTER.incrementAndGet();
        return "hello";
    }
}
