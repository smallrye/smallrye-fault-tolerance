package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.mutiny.Uni;

public class MutinyFallbackTest {
    @Test
    public void fallbackWithSupplier() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withFallback().handler(this::fallback).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void fallbackWithFunction() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withFallback().handler(e -> Uni.createFrom().item(e.getClass().getSimpleName())).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("TestException");
    }

    @Test
    public void fallbackWithSkipOn() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withFallback().handler(this::fallback).skipOn(TestException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithApplyOn() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withFallback().handler(this::fallback).applyOn(RuntimeException.class).done()
                .build()
                .adaptSupplier(this::action);

        assertThat(guarded.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithWhen() {
        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withFallback().handler(this::fallback).when(e -> e instanceof RuntimeException).done()
                .build()
                .adaptSupplier(this::action);

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
