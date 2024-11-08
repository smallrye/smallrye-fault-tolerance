package io.smallrye.faulttolerance.mutiny.test;

import static io.smallrye.faulttolerance.core.util.Timing.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.mutiny.Uni;

public class MutinyTimeoutTest {
    @Test
    public void nonblockingAsyncTimeout() throws Exception {
        Callable<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withTimeout().duration(1, ChronoUnit.SECONDS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::nonblockingAction);

        long time = timed(() -> {
            assertThat(guarded.call().subscribeAsCompletionStage())
                    .succeedsWithin(5, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(20));
    }

    @Test
    public void blockingAsyncTimeout() throws Exception {
        Callable<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withTimeout().duration(1, ChronoUnit.SECONDS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .withThreadOffload(true) // async timeout doesn't interrupt the running thread
                .build()
                .adaptCallable(this::blockingAction);

        long time = timed(() -> {
            assertThat(guarded.call().subscribeAsCompletionStage())
                    .succeedsWithin(5, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(20));
    }

    public Uni<String> nonblockingAction() {
        return Uni.createFrom().item("value")
                .onItem().delayIt().by(Duration.ofSeconds(10));
    }

    public Uni<String> blockingAction() throws InterruptedException {
        Thread.sleep(10_000);
        return Uni.createFrom().item("value");
    }

    public Uni<String> fallback() {
        return Uni.createFrom().item("fallback");
    }
}
