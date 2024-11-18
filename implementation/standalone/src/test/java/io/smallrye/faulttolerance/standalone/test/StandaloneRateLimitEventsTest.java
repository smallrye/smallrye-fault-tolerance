package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.api.TypedGuard;

public class StandaloneRateLimitEventsTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void rateLimit() throws Exception {
        AtomicInteger permittedCounter = new AtomicInteger();
        AtomicInteger rejectedCounter = new AtomicInteger();

        TypedGuard<String> guarded = TypedGuard.create(String.class)
                .withRateLimit()
                .limit(5)
                .window(1, ChronoUnit.MINUTES)
                .onPermitted(permittedCounter::incrementAndGet)
                .onRejected(rejectedCounter::incrementAndGet)
                .done()
                .withFallback().handler(this::fallback).applyOn(RateLimitException.class).done()
                .build();

        List<Future<String>> futures = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                return guarded.call(() -> {
                    return "hello";
                });
            }));
        }

        List<String> results = new ArrayList<>(10);
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(10);

        assertThat(results).filteredOn("hello"::equals).hasSize(5);
        assertThat(results).filteredOn("fallback"::equals).hasSize(5);

        assertThat(permittedCounter).hasValue(5);
        assertThat(rejectedCounter).hasValue(5);
    }

    public String fallback() {
        return "fallback";
    }
}
