package io.smallrye.faulttolerance.vertx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.vertx.core.Future;

public class VertxFutureFallbackTest {
    @Test
    public void fallbackWithSupplier() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void fallbackWithFunction() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withFallback().handler(e -> Future.succeededFuture(e.getClass().getSimpleName())).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("TestException");
    }

    @Test
    public void fallbackWithSkipOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withFallback().handler(this::fallback).skipOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithApplyOn() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withFallback().handler(this::fallback).applyOn(RuntimeException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithWhen() {
        Supplier<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withFallback().handler(this::fallback).when(e -> e instanceof RuntimeException).done()
                .build()
                .adaptSupplier(this::action);

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
