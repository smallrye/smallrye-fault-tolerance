package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneCircuitBreakerAsyncTest {
    @BeforeEach
    public void setUp() {
        FaultTolerance.circuitBreakerMaintenance().resetAll();
    }

    @Test
    public void asyncCircuitBreaker() throws Exception {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void asyncCircuitBreakerWithSkipOn() throws Exception {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).skipOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncCircuitBreakerWithFailOn() throws Exception {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).failOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    public CompletionStage<String> action() {
        return failedStage(new TestException());
    }

    public CompletionStage<String> fallback() {
        return completedStage("fallback");
    }
}
