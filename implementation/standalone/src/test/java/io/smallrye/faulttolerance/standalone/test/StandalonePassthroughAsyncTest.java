package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static io.smallrye.faulttolerance.core.util.CompletionStages.failedStage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandalonePassthroughAsyncTest {
    @Test
    public void asyncPassthroughCompletedSuccessfully() throws Exception {
        FaultTolerance<CompletionStage<String>> guard = FaultTolerance.<String> createAsync().build();

        assertThat(guard.call(this::completeSuccessfully))
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void asyncPassthroughCompletedExceptionally() throws Exception {
        FaultTolerance<CompletionStage<String>> guard = FaultTolerance.<String> createAsync().build();

        assertThat(guard.call(this::completeExceptionally))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncPassthroughThrownException() throws Exception {
        FaultTolerance<CompletionStage<String>> guard = FaultTolerance.<String> createAsync().build();

        assertThat(guard.call(this::throwException))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callableAsyncPassthroughCompletedSuccessfully() throws Exception {
        Callable<CompletionStage<String>> guard = FaultTolerance.createAsyncCallable(this::completeSuccessfully).build();

        assertThat(guard.call())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void callableAsyncPassthroughCompletedExceptionally() throws Exception {
        Callable<CompletionStage<String>> guard = FaultTolerance.createAsyncCallable(this::completeExceptionally).build();

        assertThat(guard.call())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callableAsyncPassthroughThrownException() throws Exception {
        Callable<CompletionStage<String>> guard = FaultTolerance.createAsyncCallable(this::throwException).build();

        assertThat(guard.call())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierAsyncPassthroughCompletedSuccessfully() {
        Supplier<CompletionStage<String>> guard = FaultTolerance.createAsyncSupplier(this::completeSuccessfully).build();

        assertThat(guard.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void supplierAsyncPassthroughCompletedExceptionally() {
        Supplier<CompletionStage<String>> guard = FaultTolerance.createAsyncSupplier(this::completeExceptionally).build();

        assertThat(guard.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierAsyncPassthroughThrownException() {
        Supplier<CompletionStage<String>> guard = FaultTolerance.createAsyncSupplier(this::throwRuntimeException).build();

        assertThat(guard.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void runnableAsyncPassthroughCompletedSuccessfully() {
        Runnable guard = FaultTolerance.createAsyncRunnable(this::completeSuccessfully).build();

        assertThatCode(guard::run).doesNotThrowAnyException();
    }

    @Test
    public void runnableAsyncPassthroughCompletedExceptionally() {
        Runnable guard = FaultTolerance.createAsyncRunnable(this::completeExceptionally).build();

        assertThatCode(guard::run).doesNotThrowAnyException();
    }

    @Test
    public void runnableAsyncPassthroughThrownException() {
        Runnable guard = FaultTolerance.createAsyncRunnable(this::throwRuntimeException).build();

        // internally, a CompletableFuture is created that is completed with the thrown exception
        // and since the action is a Runnable, that CompletableFuture is just dropped
        // (in other words, "async runnable" is just "fire and forget")
        assertThatCode(guard::run).doesNotThrowAnyException();
    }

    private CompletionStage<String> completeSuccessfully() {
        return completedStage("value");
    }

    private CompletionStage<String> completeExceptionally() {
        return failedStage(new TestException());
    }

    private CompletionStage<String> throwException() throws TestException {
        throw new TestException();
    }

    private CompletionStage<String> throwRuntimeException() {
        throw new RuntimeException(new TestException());
    }
}
