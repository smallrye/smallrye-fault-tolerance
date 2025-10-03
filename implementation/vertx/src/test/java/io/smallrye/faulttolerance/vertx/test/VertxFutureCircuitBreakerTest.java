package io.smallrye.faulttolerance.vertx.test;

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
import io.vertx.core.Future;

public class VertxFutureCircuitBreakerTest {
    @BeforeEach
    public void setUp() {
        CircuitBreakerMaintenance.get().resetAll();
    }

    @Test
    public void circuitBreaker() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void circuitBreakerWithSkipOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).skipOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithFailOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).failOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithWhen() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withCircuitBreaker().requestVolumeThreshold(4).when(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof CircuitBreakerOpenException).done()
                .build()
                .adaptSupplier(this::action);

        for (int i = 0; i < 4; i++) {
            assertThat(guarded.get().toCompletionStage())
                    .failsWithin(10, TimeUnit.SECONDS)
                    .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                    .withCauseExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    public Future<String> action() {
        return Future.failedFuture(new TestException());
    }

    public Future<String> fallback() {
        return Future.succeededFuture("fallback");
    }
}
