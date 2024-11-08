package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.mutiny.Uni;

public class MutinyPassthroughTest {
    @Test
    public void passthroughCompletedSuccessfully() throws Exception {
        TypedGuard<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING).build();

        assertThat(guard.call(this::completeSuccessfully).subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void passthroughCompletedExceptionally() throws Exception {
        TypedGuard<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING).build();

        assertThat(guard.call(this::completeExceptionally).subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void passthroughThrownException() throws Exception {
        TypedGuard<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING).build();

        assertThat(guard.call(this::throwException).subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughCompletedSuccessfully() throws Exception {
        Callable<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptCallable(this::completeSuccessfully);

        assertThat(guard.call().subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void callablePassthroughCompletedExceptionally() throws Exception {
        Callable<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptCallable(this::completeExceptionally);

        assertThat(guard.call().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughThrownException() throws Exception {
        Callable<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptCallable(this::throwException);

        assertThat(guard.call().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughCompletedSuccessfully() {
        Supplier<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptSupplier(this::completeSuccessfully);

        assertThat(guard.get().subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("value");
    }

    @Test
    public void supplierPassthroughCompletedExceptionally() {
        Supplier<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptSupplier(this::completeExceptionally);

        assertThat(guard.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughThrownException() {
        Supplier<Uni<String>> guard = TypedGuard.create(Types.UNI_STRING)
                .build()
                .adaptSupplier(this::throwRuntimeException);

        assertThat(guard.get().subscribeAsCompletionStage())
                .failsWithin(10, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class) // caused by AssertJ calling future.get()
                .withCauseExactlyInstanceOf(RuntimeException.class);
    }

    private Uni<String> completeSuccessfully() {
        return Uni.createFrom().item("value");
    }

    private Uni<String> completeExceptionally() {
        return Uni.createFrom().failure(new TestException());
    }

    private Uni<String> throwException() throws TestException {
        throw new TestException();
    }

    private Uni<String> throwRuntimeException() {
        throw new RuntimeException(new TestException());
    }
}
