package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneFaultToleranceCastAsyncTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(6);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void castingAsyncCircuitBreaker() throws Exception {
        FaultTolerance<CompletionStage<String>> guardedString = FaultTolerance.<String> createAsync()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();
        FaultTolerance<CompletionStage<Integer>> guardedInteger = guardedString.castAsync(CompletionStage.class);

        for (int i = 0; i < 3; i++) {
            assertThat(guardedString.call(this::stringAction))
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);

            assertThat(guardedInteger.call(this::integerAction))
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guardedString.call(this::stringAction))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void castingAsyncBulkhead() throws Exception {
        FaultTolerance<CompletionStage<String>> guardedString = FaultTolerance.<String> createAsync()
                .withBulkhead().limit(6).queueSize(2).done()
                .withThreadOffload(true)
                .build();
        FaultTolerance<CompletionStage<Integer>> guardedInteger = guardedString.castAsync(CompletionStage.class);

        Party party = Party.create(6);

        for (int i = 0; i < 4; i++) {
            guardedString.call(() -> {
                party.participant().attend();
                return completedFuture("ignored");
            });
            guardedInteger.call(() -> {
                party.participant().attend();
                return completedFuture(42);
            });
        }

        party.organizer().waitForAll();

        assertThat(guardedString.call(() -> completedFuture("value")))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void castingAsyncRateLimit() throws Exception {
        FaultTolerance<CompletionStage<String>> guardedString = FaultTolerance.<String> createAsync()
                .withRateLimit().limit(6).window(1, ChronoUnit.MINUTES).done()
                .withThreadOffload(true)
                .build();
        FaultTolerance<CompletionStage<Integer>> guardedInteger = guardedString.castAsync(CompletionStage.class);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guardedString.call(() -> completedFuture("ignored")).toCompletableFuture().get();
            }));
            futures.add(executor.submit(() -> {
                return guardedInteger.call(() -> completedFuture(42)).toCompletableFuture().get();
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        assertThat(guardedString.call(() -> completedFuture("value")))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void castingAsyncFallback() {
        FaultTolerance<CompletionStage<String>> guarded = FaultTolerance.<String> createAsync()
                .withFallback().handler(() -> completedFuture("fallback")).done()
                .build();

        assertThatCode(() -> guarded.castAsync(CompletionStage.class)).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void castingToSync() {
        FaultTolerance<CompletionStage<String>> guarded = FaultTolerance.<String> createAsync().build();

        assertThatCode(guarded::cast).isExactlyInstanceOf(IllegalStateException.class);
    }

    public CompletionStage<String> stringAction() {
        return failedFuture(new TestException());
    }

    public CompletionStage<Integer> integerAction() {
        return failedFuture(new TestException());
    }
}
