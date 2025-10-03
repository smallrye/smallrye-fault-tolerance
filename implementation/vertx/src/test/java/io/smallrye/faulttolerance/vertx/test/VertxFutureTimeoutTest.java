package io.smallrye.faulttolerance.vertx.test;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static io.smallrye.faulttolerance.core.util.Timing.timed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class VertxFutureTimeoutTest {
    @Test
    public void nonblockingAsyncTimeout() throws Exception {
        Callable<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withTimeout().duration(1, ChronoUnit.SECONDS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .build()
                .adaptCallable(this::nonblockingAction);

        long time = timed(() -> {
            assertThat(guarded.call().toCompletionStage())
                    .succeedsWithin(5, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(20));
    }

    @Test
    public void blockingAsyncTimeout() throws Exception {
        Callable<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withTimeout().duration(1, ChronoUnit.SECONDS).done()
                .withFallback().applyOn(TimeoutException.class).handler(this::fallback).done()
                .withThreadOffload(true) // async timeout doesn't interrupt the running thread
                .build()
                .adaptCallable(this::blockingAction);

        long time = timed(() -> {
            assertThat(guarded.call().toCompletionStage())
                    .succeedsWithin(5, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        });
        assertThat(time).isCloseTo(1000, withinPercentage(20));
    }

    public Future<String> nonblockingAction() {
        Promise<String> promise = Promise.promise();
        new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                throw sneakyThrow(e);
            }

            promise.complete("value");
        });
        return promise.future();
    }

    public Future<String> blockingAction() throws InterruptedException {
        Thread.sleep(10_000);
        return Future.succeededFuture("value");
    }

    public Future<String> fallback() {
        return Future.succeededFuture("fallback");
    }
}
