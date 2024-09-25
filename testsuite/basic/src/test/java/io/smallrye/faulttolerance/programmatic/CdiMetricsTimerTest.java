package io.smallrye.faulttolerance.programmatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.temporal.ChronoUnit;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.metrics.MetricsConstants;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

// needs to stay in sync with `StandaloneMetricsTimerTest`
@FaultToleranceBasicTest
public class CdiMetricsTimerTest {
    static Barrier barrier;

    @BeforeAll
    public static void setUp() {
        barrier = Barrier.interruptible();
    }

    @Test
    public void test(@RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) throws Exception {
        Callable<CompletionStage<String>> guarded = FaultTolerance.createAsyncCallable(this::action)
                .withThreadOffload(true)
                .withTimeout().duration(1, ChronoUnit.MINUTES).done()
                .withFallback().handler(this::fallback).done()
                .build();

        CompletableFuture<String> future = guarded.call().toCompletableFuture();

        assertThat(future).isNotCompleted();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(findTimerGauge(metrics).getValue()).isEqualTo(1);
        });

        barrier.open();

        assertThat(future).succeedsWithin(2, TimeUnit.SECONDS)
                .isEqualTo("hello");

        assertThat(findTimerGauge(metrics).getValue()).isEqualTo(0);
    }

    public CompletionStage<String> action() throws InterruptedException {
        barrier.await();
        return CompletableFuture.completedFuture("hello");
    }

    public CompletionStage<String> fallback() {
        return CompletableFuture.completedFuture("fallback");
    }

    private static Gauge<?> findTimerGauge(MetricRegistry metrics) {
        SortedMap<MetricID, Gauge> timers = metrics.getGauges(
                (id, metric) -> id.getName().equals(MetricsConstants.TIMER_SCHEDULED));
        assertThat(timers).hasSize(1);
        return timers.values().iterator().next();
    }
}
