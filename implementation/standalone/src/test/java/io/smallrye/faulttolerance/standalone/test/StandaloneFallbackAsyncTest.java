package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneFallbackAsyncTest {
    @Test
    public void asyncFallbackWithSupplier() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void asyncFallbackWithFunction() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withFallback().handler(e -> completedFuture(e.getClass().getSimpleName())).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("TestException");
    }

    @Test
    public void asyncFallbackWithSkipOn() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withFallback().handler(this::fallback).skipOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncFallbackWithApplyOn() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withFallback().handler(this::fallback).applyOn(RuntimeException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncFallbackWithWhen() {
        Supplier<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withFallback().handler(this::fallback).when(e -> e instanceof RuntimeException).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    public CompletionStage<String> action() {
        return failedFuture(new TestException());
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
