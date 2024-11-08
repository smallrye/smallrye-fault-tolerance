package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandalonePassthroughAsyncTest {
    @Test
    public void asyncPassthroughCompletedSuccessfully() throws Exception {
        TypedGuard<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build();

        assertThat(guard.call(this::completeSuccessfully))
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void asyncPassthroughCompletedExceptionally() throws Exception {
        TypedGuard<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build();

        assertThat(guard.call(this::completeExceptionally))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void asyncPassthroughThrownException() throws Exception {
        TypedGuard<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build();

        assertThat(guard.call(this::throwException))
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callableAsyncPassthroughCompletedSuccessfully() throws Exception {
        Callable<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptCallable(this::completeSuccessfully);

        assertThat(guard.call())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void callableAsyncPassthroughCompletedExceptionally() throws Exception {
        Callable<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptCallable(this::completeExceptionally);

        assertThat(guard.call())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callableAsyncPassthroughThrownException() throws Exception {
        Callable<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptCallable(this::throwException);

        assertThat(guard.call())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierAsyncPassthroughCompletedSuccessfully() {
        Supplier<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptSupplier(this::completeSuccessfully);

        assertThat(guard.get())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void supplierAsyncPassthroughCompletedExceptionally() {
        Supplier<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptSupplier(this::completeExceptionally);

        assertThat(guard.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierAsyncPassthroughThrownException() {
        Supplier<CompletionStage<String>> guard = TypedGuard.create(Types.CS_STRING).build()
                .adaptSupplier(this::throwRuntimeException);

        assertThat(guard.get())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(RuntimeException.class);
    }

    private CompletionStage<String> completeSuccessfully() {
        return completedFuture("value");
    }

    private CompletionStage<String> completeExceptionally() {
        return failedFuture(new TestException());
    }

    private CompletionStage<String> throwException() throws TestException {
        throw new TestException();
    }

    private CompletionStage<String> throwRuntimeException() {
        throw new RuntimeException();
    }
}
