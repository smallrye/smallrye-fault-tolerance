package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.standalone.Configuration;
import io.smallrye.faulttolerance.standalone.MetricsAdapter;
import io.smallrye.faulttolerance.standalone.MicrometerAdapter;
import io.smallrye.faulttolerance.standalone.StandaloneFaultTolerance;

// needs to stay in sync with `CdiMetricsTest`
public class StandaloneMetricsTest {
    private static final String NAME = StandaloneMetricsTest.class.getName() + " programmatic usage";

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
        Callable<String> guarded = TypedGuard.create(String.class)
                .withDescription(NAME)
                .withFallback().handler(this::fallback).done()
                .withRetry().maxRetries(3).done()
                .build()
                .adaptCallable(this::action);

        assertThat(guarded.call()).isEqualTo("fallback");

        assertThat(metrics.counter(MetricsConstants.INVOCATIONS_TOTAL, List.of(
                Tag.of("method", NAME),
                Tag.of("result", "valueReturned"),
                Tag.of("fallback", "applied")))
                .count()).isEqualTo(1.0);

        assertThat(metrics.counter(MetricsConstants.RETRY_RETRIES_TOTAL, List.of(
                Tag.of("method", NAME)))
                .count()).isEqualTo(3.0);
        assertThat(metrics.counter(MetricsConstants.RETRY_CALLS_TOTAL, List.of(
                Tag.of("method", NAME),
                Tag.of("retried", "true"),
                Tag.of("retryResult", "maxRetriesReached")))
                .count()).isEqualTo(1.0);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
