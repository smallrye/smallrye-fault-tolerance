package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneCircuitBreakerAsyncEventsTest {
    private boolean actionShouldFail;

    @BeforeEach
    public void setUp() {
        FaultTolerance.circuitBreakerMaintenance().resetAll();
    }

    @Test
    public void asyncCircuitBreakerEvents() throws Exception {
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();
        AtomicInteger preventedCounter = new AtomicInteger();
        AtomicReference<CircuitBreakerState> changedState = new AtomicReference<>();

        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withCircuitBreaker()
                .requestVolumeThreshold(4)
                .onSuccess(successCounter::incrementAndGet)
                .onFailure(failureCounter::incrementAndGet)
                .onPrevented(preventedCounter::incrementAndGet)
                .onStateChange(changedState::set)
                .done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        actionShouldFail = false;
        for (int i = 0; i < 2; i++) {
            assertThat(guarded.get())
                    .succeedsWithin(10, TimeUnit.SECONDS)
                    .isEqualTo("value");
        }
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(0);
        assertThat(preventedCounter).hasValue(0);
        assertThat(changedState).hasValue(null);

        actionShouldFail = true;
        for (int i = 0; i < 2; i++) {
            assertThat(guarded.get())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(2);
        assertThat(preventedCounter).hasValue(0);
        assertThat(changedState).hasValue(CircuitBreakerState.OPEN);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(2);
        assertThat(preventedCounter).hasValue(1);
    }

    public CompletionStage<String> action() {
        if (actionShouldFail) {
            return failedFuture(new TestException());
        } else {
            return completedFuture("value");
        }
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
