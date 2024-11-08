package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

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

import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneUntypedAsyncGuardTest {
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
                return completedFuture("ignored");
            }, Types.CS_STRING);
            guard.call(() -> {
                party.participant().attend();
                return completedFuture(42);
            }, Types.CS_INTEGER);
        }

        party.organizer().waitForAll();

        assertThat(guard.call(() -> completedFuture("value"), Types.CS_STRING))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void circuitBreaker() throws Exception {
        Guard guard = Guard.create()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();

        for (int i = 0; i < 3; i++) {
            assertThat(guard.call(this::stringAction, Types.CS_STRING))
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);

            assertThat(guard.call(this::integerAction, Types.CS_INTEGER))
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guard.call(this::stringAction, Types.CS_STRING))
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

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guard.call(() -> completedFuture("ignored"), Types.CS_STRING).toCompletableFuture().get();
            }));
            futures.add(executor.submit(() -> {
                return guard.call(() -> completedFuture(42), Types.CS_INTEGER).toCompletableFuture().get();
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        assertThat(guard.call(() -> completedFuture("value"), Types.CS_STRING))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(RateLimitException.class);
    }

    public CompletionStage<String> stringAction() {
        return failedFuture(new TestException());
    }

    public CompletionStage<Integer> integerAction() {
        return failedFuture(new TestException());
    }
}
