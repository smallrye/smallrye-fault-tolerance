package io.smallrye.faulttolerance.vertx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.vertx.core.Future;

public class VertxFuturePassthroughTest {
    @Test
    public void passthroughCompletedSuccessfully() throws Exception {
        TypedGuard<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING).build();

        assertThat(guard.call(this::completeSuccessfully).toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void passthroughCompletedExceptionally() throws Exception {
        TypedGuard<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING).build();

        assertThat(guard.call(this::completeExceptionally).toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void passthroughThrownException() throws Exception {
        TypedGuard<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING).build();

        assertThat(guard.call(this::throwException).toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughCompletedSuccessfully() throws Exception {
        Callable<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptCallable(this::completeSuccessfully);

        assertThat(guard.call().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void callablePassthroughCompletedExceptionally() throws Exception {
        Callable<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptCallable(this::completeExceptionally);

        assertThat(guard.call().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughThrownException() throws Exception {
        Callable<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptCallable(this::throwException);

        assertThat(guard.call().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughCompletedSuccessfully() {
        Supplier<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptSupplier(this::completeSuccessfully);

        assertThat(guard.get().toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void supplierPassthroughCompletedExceptionally() {
        Supplier<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptSupplier(this::completeExceptionally);

        assertThat(guard.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughThrownException() {
        Supplier<Future<String>> guard = TypedGuard.create(Types.FUTURE_STRING)
                .build()
                .adaptSupplier(this::throwRuntimeException);

        assertThat(guard.get().toCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(RuntimeException.class);
    }

    private Future<String> completeSuccessfully() {
        return Future.succeededFuture("value");
    }

    private Future<String> completeExceptionally() {
        return Future.failedFuture(new TestException());
    }

    private Future<String> throwException() throws TestException {
        throw new TestException();
    }

    private Future<String> throwRuntimeException() {
        throw new RuntimeException(new TestException());
    }
}
