package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneFallbackAsyncTest {
    @Test
    public void asyncFallbackWithSupplier() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withFallback().handler(this::fallback).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");
    }

    @Test
    public void asyncFallbackWithFunction() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withFallback().handler(e -> completedStage(e.getClass().getSimpleName())).done()
                .build();

        assertThat(guarded.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("TestException");
    }

    @Test
    public void asyncFallbackWithSkipOn() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withFallback().handler(this::fallback).skipOn(TestException.class).done()
                .build();

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncFallbackWithApplyOn() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withFallback().handler(this::fallback).applyOn(RuntimeException.class).done()
                .build();

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncFallbackWithWhen() {
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createAsyncSupplier(this::action)
                .withFallback().handler(this::fallback).when(e -> e instanceof RuntimeException).done()
                .build();

        assertThat(guarded.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void synchronousFlow() {
        // doing this is usually a mistake, because it only guards the synchronous execution
        // only testing it here to verify that indeed asynchronous fault tolerance doesn't apply
        Supplier<CompletionStage<String>> guarded = FaultTolerance.createSupplier(this::action)
                .withFallback().handler(this::fallback).done()
                .build();

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
