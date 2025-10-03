package io.smallrye.faulttolerance.vertx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.party.Party;
import io.vertx.core.Future;

public class VertxFutureUntypedGuardTest {
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
    public void bulkhead() throws Exception {
        Guard guard = Guard.create()
                .withBulkhead().limit(6).queueSize(2).done()
                .withThreadOffload(true)
                .build();

        Party party = Party.create(6);

        for (int i = 0; i < 4; i++) {
            guard.call(() -> {
                party.participant().attend();
                return io.vertx.core.Future.succeededFuture("ignored");
            }, Types.FUTURE_STRING);
            guard.call(() -> {
                party.participant().attend();
                return io.vertx.core.Future.succeededFuture(42);
            }, Types.FUTURE_INTEGER);
        }

        party.organizer().waitForAll();

        assertThat(guard.call(() -> Future.succeededFuture("value"), Types.FUTURE_STRING).toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void circuitBreaker() throws Exception {
        Guard guard = Guard.create()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();

        for (int i = 0; i < 3; i++) {
            assertThat(guard.call(this::stringAction, Types.FUTURE_STRING).toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);

            assertThat(guard.call(this::integerAction, Types.FUTURE_INTEGER).toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guard.call(this::stringAction, Types.FUTURE_STRING).toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void rateLimit() throws Exception {
        Guard guard = Guard.create()
                .withRateLimit().limit(6).window(1, ChronoUnit.MINUTES).done()
                .withThreadOffload(true)
                .build();

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guard.call(() -> Future.succeededFuture("ignored"), Types.FUTURE_STRING)
                        .toCompletionStage().toCompletableFuture().join();
            }));
            futures.add(executor.submit(() -> {
                return guard.call(() -> Future.succeededFuture(42), Types.FUTURE_INTEGER)
                        .toCompletionStage().toCompletableFuture().join();
            }));
        }

        for (java.util.concurrent.Future<?> future : futures) {
            future.get();
        }

        assertThat(guard.call(() -> Future.succeededFuture("value"), Types.FUTURE_STRING).toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(RateLimitException.class);
    }

    public Future<String> stringAction() {
        return Future.failedFuture(new TestException());
    }

    public Future<Integer> integerAction() {
        return Future.failedFuture(new TestException());
    }
}
