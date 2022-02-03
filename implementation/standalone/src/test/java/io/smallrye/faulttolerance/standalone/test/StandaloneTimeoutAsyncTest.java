package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.Timing.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;

public class StandaloneTimeoutAsyncTest {
    @Test
    public void asyncTimeout() throws Exception {
        Callable<CompletionStage<String>> guarded = FaultTolerance.createAsyncCallable(this::action)
                .withTimeout().duration(1, ChronoUnit.SECONDS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .withThreadOffload(true) // async timeout doesn't interrupt the running thread
                .build();

        long time = timed(() -> {
            assertThat(guarded.call())
                    .succeedsWithin(5, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(50));
    }

    public CompletionStage<String> action() throws InterruptedException {
        Thread.sleep(10_000);
        return completedStage("value");
    }

    public CompletionStage<String> fallback() {
        return completedStage("fallback");
    }
}
