package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

public class StandaloneUntypedGuardTest {
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
                .withBulkhead().limit(6).done()
                .build();

        Party party = Party.create(6);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                return guard.call(() -> {
                    party.participant().attend();
                    return "ignored";
                }, String.class);
            });
            executor.submit(() -> {
                return guard.call(() -> {
                    party.participant().attend();
                    return 42;
                }, Integer.class);
            });
        }

        party.organizer().waitForAll();

        assertThatCode(() -> guard.call(() -> "value", String.class)).isExactlyInstanceOf(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void circuitBreaker() {
        Guard guarded = Guard.create()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();

        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> guarded.call(this::stringAction, String.class)).isExactlyInstanceOf(TestException.class);
            assertThatCode(() -> guarded.call(this::integerAction, Integer.class)).isExactlyInstanceOf(TestException.class);
        }

        assertThatCode(() -> guarded.call(this::stringAction, String.class))
                .isExactlyInstanceOf(CircuitBreakerOpenException.class);
    }

    @Test
    public void rateLimit() throws ExecutionException, InterruptedException {
        Guard guard = Guard.create()
                .withRateLimit().limit(6).window(1, ChronoUnit.MINUTES).done()
                .build();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guard.call(() -> "ignored", String.class);
            }));
            futures.add(executor.submit(() -> {
                return guard.call(() -> 42, Integer.class);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        assertThatCode(() -> guard.call(() -> "value", String.class)).isExactlyInstanceOf(RateLimitException.class);
    }

    public String stringAction() throws TestException {
        throw new TestException();
    }

    public Integer integerAction() throws TestException {
        throw new TestException();
    }
}
