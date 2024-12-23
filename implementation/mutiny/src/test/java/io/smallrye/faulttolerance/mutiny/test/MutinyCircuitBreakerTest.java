package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.mutiny.Uni;

public class MutinyCircuitBreakerTest {
    @BeforeEach
    public void setUp() {
        CircuitBreakerMaintenance.get().resetAll();
    }

    @Test
    public void circuitBreaker() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().subscribeAsCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void circuitBreakerWithSkipOn() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).skipOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().subscribeAsCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithFailOn() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).failOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().subscribeAsCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithWhen() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).when(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof CircuitBreakerOpenException).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().subscribeAsCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    public Uni<String> action() {
        return Uni.createFrom().failure(new TestException());
    }

    public Uni<String> fallback() {
        return Uni.createFrom().item("fallback");
    }
}
