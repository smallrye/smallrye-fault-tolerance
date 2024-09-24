package io.smallrye.faulttolerance.standalone.test;

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

public class StandaloneFaultToleranceCastTest {
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
    public void castingCircuitBreaker() {
        FaultTolerance<String> guardedString = FaultTolerance.<String> create()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();
        FaultTolerance<Integer> guardedInteger = guardedString.cast();

        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> guardedString.call(this::stringAction)).isExactlyInstanceOf(TestException.class);
            assertThatCode(() -> guardedInteger.call(this::integerAction)).isExactlyInstanceOf(TestException.class);
        }

        assertThatCode(() -> guardedString.call(this::stringAction)).isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void castingBulkhead() throws Exception {
        FaultTolerance<String> guardedString = FaultTolerance.<String> create()
                .withBulkhead().limit(6).done()
                .build();
        FaultTolerance<Integer> guardedInteger = guardedString.cast();

        Party party = Party.create(6);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                return guardedString.call(() -> {
                    party.participant().attend();
                    return "ignored";
                });
            });
            executor.submit(() -> {
                return guardedInteger.call(() -> {
                    party.participant().attend();
                    return 42;
                });
            });
        }

        party.organizer().waitForAll();

        assertThatCode(() -> guardedString.call(() -> "value")).isExactlyInstanceOf(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void castingRateLimit() throws ExecutionException, InterruptedException {
        FaultTolerance<String> guardedString = FaultTolerance.<String> create()
                .withRateLimit().limit(6).window(1, ChronoUnit.MINUTES).done()
                .build();
        FaultTolerance<Integer> guardedInteger = guardedString.cast();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guardedString.call(() -> "ignored");
            }));
            futures.add(executor.submit(() -> {
                return guardedInteger.call(() -> 42);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        assertThatCode(() -> guardedString.call(() -> "value")).isExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void castingFallback() {
        FaultTolerance<String> guarded = FaultTolerance.<String> create()
                .withFallback().handler(() -> "fallback").done()
                .build();
        assertThatCode(guarded::cast).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void castingToAsync() {
        FaultTolerance<String> guarded = FaultTolerance.<String> create().build();
        assertThatCode(() -> guarded.castAsync(CompletionStage.class)).isExactlyInstanceOf(IllegalStateException.class);
    }

    public String stringAction() throws TestException {
        throw new TestException();
    }

    public Integer integerAction() throws TestException {
        throw new TestException();
    }
}
