package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.standalone.Configuration;
import io.smallrye.faulttolerance.standalone.MetricsAdapter;
import io.smallrye.faulttolerance.standalone.MicrometerAdapter;
import io.smallrye.faulttolerance.standalone.StandaloneFaultTolerance;

// needs to stay in sync with `CdiMetricsTimerTest`
public class StandaloneMetricsTimerTest {
    static ExecutorService executor;
    static MeterRegistry metrics;

    static Barrier barrier;

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

        barrier = Barrier.interruptible();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        StandaloneFaultTolerance.shutdown();
    }

    @Test
    public void test() throws Exception {
        Callable<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withThreadOffload(true)
                .withTimeout().duration(1, ChronoUnit.MINUTES).done()
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptCallable(this::action);

        CompletableFuture<String> future = guarded.call().toCompletableFuture();

        assertThat(future).isNotCompleted();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(metrics.get(MetricsConstants.TIMER_SCHEDULED).gauge().value()).isEqualTo(1.0);
        });

        barrier.open();

        assertThat(future).succeedsWithin(2, TimeUnit.SECONDS)
                .isEqualTo("hello");

        assertThat(metrics.get(MetricsConstants.TIMER_SCHEDULED).gauge().value()).isEqualTo(0.0);
    }

    public CompletionStage<String> action() throws InterruptedException {
        barrier.await();
        return CompletableFuture.completedFuture("hello");
    }

    public CompletionStage<String> fallback() {
        return CompletableFuture.completedFuture("fallback");
    }
}
