package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
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
import io.smallrye.faulttolerance.mutiny.api.MutinyFaultTolerance;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class MutinyFaultToleranceCastTest {
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
    public void castingCircuitBreaker() throws Exception {
        FaultTolerance<Uni<String>> guardedString = MutinyFaultTolerance.<String> create()
                .withCircuitBreaker().requestVolumeThreshold(6).done()
                .build();
        FaultTolerance<Uni<Integer>> guardedInteger = guardedString.castAsync(Uni.class);

        for (int i = 0; i < 3; i++) {
            guardedString.call(this::stringAction)
                    .subscribe().withSubscriber(new UniAssertSubscriber<>())
                    .awaitFailure(Duration.ofSeconds(10))
                    .assertFailedWith(TestException.class);

            guardedInteger.call(this::integerAction)
                    .subscribe().withSubscriber(new UniAssertSubscriber<>())
                    .awaitFailure(Duration.ofSeconds(10))
                    .assertFailedWith(TestException.class);
        }

        guardedString.call(this::stringAction)
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .awaitFailure(Duration.ofSeconds(10))
                .assertFailedWith(CircuitBreakerOpenException.class);
    }

    @Test
    public void castingBulkhead() throws Exception {
        FaultTolerance<Uni<String>> guardedString = MutinyFaultTolerance.<String> create()
                .withBulkhead().limit(6).queueSize(2).done()
                .withThreadOffload(true)
                .build();
        FaultTolerance<Uni<Integer>> guardedInteger = guardedString.castAsync(Uni.class);

        Party party = Party.create(6);

        for (int i = 0; i < 4; i++) {
            guardedString.call(() -> {
                party.participant().attend();
                return Uni.createFrom().item("ignored");
            }).subscribe().asCompletionStage();
            guardedInteger.call(() -> {
                party.participant().attend();
                return Uni.createFrom().item(42);
            }).subscribe().asCompletionStage();
        }

        party.organizer().waitForAll();

        guardedString.call(() -> Uni.createFrom().item("value"))
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .awaitFailure(Duration.ofSeconds(10))
                .assertFailedWith(BulkheadException.class);

        party.organizer().disband();
    }

    @Test
    public void castingRateLimit() throws Exception {
        FaultTolerance<Uni<String>> guardedString = MutinyFaultTolerance.<String> create()
                .withRateLimit().limit(6).window(1, ChronoUnit.MINUTES).done()
                .withThreadOffload(true)
                .build();
        FaultTolerance<Uni<Integer>> guardedInteger = guardedString.castAsync(Uni.class);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(executor.submit(() -> {
                return guardedString.call(() -> Uni.createFrom().item("ignored")).await().indefinitely();
            }));
            futures.add(executor.submit(() -> {
                return guardedInteger.call(() -> Uni.createFrom().item(42)).await().indefinitely();
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        guardedString.call(() -> Uni.createFrom().item("value"))
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .awaitFailure(Duration.ofSeconds(10))
                .assertFailedWith(RateLimitException.class);
    }

    @Test
    public void castingFallback() {
        FaultTolerance<Uni<String>> guarded = MutinyFaultTolerance.<String> create()
                .withFallback().handler(() -> Uni.createFrom().item("fallback")).done()
                .build();

        assertThatCode(() -> guarded.castAsync(Uni.class)).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void castingToSync() {
        FaultTolerance<Uni<String>> guarded = MutinyFaultTolerance.<String> create().build();

        assertThatCode(guarded::cast).isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void castingToDifferentAsync() {
        FaultTolerance<Uni<String>> guarded = MutinyFaultTolerance.<String> create().build();

        assertThatCode(() -> guarded.castAsync(CompletionStage.class)).isExactlyInstanceOf(IllegalStateException.class);
    }

    public Uni<String> stringAction() {
        return Uni.createFrom().failure(new TestException());
    }

    public Uni<Integer> integerAction() {
        return Uni.createFrom().failure(new TestException());
    }
}
