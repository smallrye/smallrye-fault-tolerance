package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.standalone.Configuration;
import io.smallrye.faulttolerance.standalone.MetricsAdapter;
import io.smallrye.faulttolerance.standalone.MicrometerAdapter;
import io.smallrye.faulttolerance.standalone.StandaloneFaultTolerance;

public class StandaloneSameMetricsIdTest {
    // all guards use this description, hence their metrics are merged
    static final String DESCRIPTION = StandaloneSameMetricsIdTest.class.getName() + " shared description";

    static ExecutorService executor;
    static MeterRegistry metrics;

    @BeforeAll
    public static void setUp() {
        executor = Executors.newCachedThreadPool();
        metrics = new SimpleMeterRegistry();

        StandaloneFaultTolerance.configure(new Configuration() {
            @Override
            public ExecutorService executor() {
                return executor;
            }

            @Override
            public MetricsAdapter metricsAdapter() {
                return new MicrometerAdapter(metrics);
            }

            @Override
            public void onShutdown() throws InterruptedException {
                metrics.close();

                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
        });
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        StandaloneFaultTolerance.shutdown();
    }

    @Test
    public void test() throws Exception {
        AtomicInteger fallbackCounter = new AtomicInteger();
        AtomicInteger retryCounter = new AtomicInteger();
        AtomicInteger circuitBreakerCounter = new AtomicInteger();
        AtomicInteger rateLimitCounter = new AtomicInteger();
        AtomicInteger timeoutCounter = new AtomicInteger();
        AtomicInteger bulkheadCounter = new AtomicInteger();

        Callable<String> fallback = TypedGuard.create(String.class)
                .withDescription(DESCRIPTION)
                .withFallback().handler(() -> "hello").done()
                .build()
                .adaptCallable(() -> {
                    fallbackCounter.incrementAndGet();
                    throw new IllegalStateException();
                });

        Callable<String> retry = TypedGuard.create(String.class)
                .withDescription(DESCRIPTION)
                .withRetry().maxRetries(3).jitter(0, ChronoUnit.MILLIS).done()
                .build()
                .adaptCallable(() -> {
                    if (retryCounter.incrementAndGet() < 3) {
                        throw new IllegalStateException();
                    }
                    return "hello";
                });

        Callable<String> circuitBreaker = Guard.create()
                .withDescription(DESCRIPTION)
                .withCircuitBreaker().done()
                .build()
                .adaptCallable(() -> {
                    circuitBreakerCounter.incrementAndGet();
                    return "hello";
                }, String.class);

        Callable<String> rateLimit = Guard.create()
                .withDescription(DESCRIPTION)
                .withRateLimit().done()
                .build()
                .adaptCallable(() -> {
                    rateLimitCounter.incrementAndGet();
                    return "hello";
                }, String.class);

        Callable<String> timeout = Guard.create()
                .withDescription(DESCRIPTION)
                .withTimeout().done()
                .build()
                .adaptCallable(() -> {
                    timeoutCounter.incrementAndGet();
                    return "hello";
                }, String.class);

        Callable<String> bulkhead = Guard.create()
                .withDescription(DESCRIPTION)
                .withBulkhead().done()
                .build()
                .adaptCallable(() -> {
                    bulkheadCounter.incrementAndGet();
                    return "hello";
                }, String.class);

        assertThat(fallback.call()).isEqualTo("hello");
        assertThat(retry.call()).isEqualTo("hello");
        assertThat(circuitBreaker.call()).isEqualTo("hello");
        assertThat(rateLimit.call()).isEqualTo("hello");
        assertThat(timeout.call()).isEqualTo("hello");
        assertThat(bulkhead.call()).isEqualTo("hello");

        assertThat(fallbackCounter.get()).isEqualTo(1);
        assertThat(retryCounter.get()).isEqualTo(3);
        assertThat(circuitBreakerCounter.get()).isEqualTo(1);
        assertThat(rateLimitCounter.get()).isEqualTo(1);
        assertThat(timeoutCounter.get()).isEqualTo(1);
        assertThat(bulkheadCounter.get()).isEqualTo(1);

        // Even though the six guards use six different strategies, they all report the same
        // `method` tag, so the general `ft.invocations.total` metric collapses into a single
        // series per [result, fallback] tag combination. Only the fallback guard differs by
        // having `fallback=applied`; the other five succeed with `fallback=notDefined` and merge.

        assertThat(metrics.counter(MetricsConstants.INVOCATIONS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("result", "valueReturned"),
                Tag.of("fallback", "applied")))
                .count()).isEqualTo(1.0);

        assertThat(metrics.counter(MetricsConstants.INVOCATIONS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("result", "valueReturned"),
                Tag.of("fallback", "notDefined")))
                .count()).isEqualTo(5.0);

        assertThat(metrics.find("ft.invocations.total").counters()).filteredOn(c -> c.count() > 0).hasSize(2);

        // The strategy-specific metrics don't collide, because each strategy is only used by
        // a single guard and hence carries a distinct set of tags.

        // retry
        assertThat(metrics.counter(MetricsConstants.RETRY_RETRIES_TOTAL, List.of(
                Tag.of("method", DESCRIPTION)))
                .count()).isEqualTo(2.0);
        assertThat(metrics.counter(MetricsConstants.RETRY_CALLS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("retried", "true"),
                Tag.of("retryResult", "valueReturned")))
                .count()).isEqualTo(1.0);

        // circuit breaker
        assertThat(metrics.counter(MetricsConstants.CIRCUIT_BREAKER_CALLS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("circuitBreakerResult", "success")))
                .count()).isEqualTo(1.0);

        // rate limit
        assertThat(metrics.counter(MetricsConstants.RATE_LIMIT_CALLS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("rateLimitResult", "permitted")))
                .count()).isEqualTo(1.0);

        // timeout
        assertThat(metrics.counter(MetricsConstants.TIMEOUT_CALLS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("timedOut", "false")))
                .count()).isEqualTo(1.0);

        // bulkhead
        assertThat(metrics.counter(MetricsConstants.BULKHEAD_CALLS_TOTAL, List.of(
                Tag.of("method", DESCRIPTION),
                Tag.of("bulkheadResult", "accepted")))
                .count()).isEqualTo(1.0);
    }
}
