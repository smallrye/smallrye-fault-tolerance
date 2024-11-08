package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandalonePassthroughTest {
    @Test
    public void passthroughValue() throws Exception {
        TypedGuard<String> guard = TypedGuard.create(String.class).build();

        assertThat(guard.call(this::returnValue)).isEqualTo("value");
        assertThat(guard.get(this::returnValue)).isEqualTo("value");
    }

    @Test
    public void passthroughRuntimeException() {
        TypedGuard<Object> guard = TypedGuard.create(Object.class).build();

        assertThatCode(() -> guard.call(this::throwRuntimeException))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);

        assertThatCode(() -> guard.get(this::throwRuntimeException))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void passthroughException() {
        TypedGuard<Object> guard = TypedGuard.create(Object.class).build();

        assertThatCode(() -> guard.call(this::throwException))
                .isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughValue() throws Exception {
        Callable<String> guard = TypedGuard.create(String.class).build()
                .adaptCallable(this::returnValue);

        assertThat(guard.call()).isEqualTo("value");
    }

    @Test
    public void callablePassthroughRuntimeException() {
        Callable<String> guard = TypedGuard.create(String.class).build()
                .adaptCallable(this::throwRuntimeException);

        assertThatCode(guard::call)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughException() {
        Callable<String> guard = TypedGuard.create(String.class).build()
                .adaptCallable(this::throwException);

        assertThatCode(guard::call)
                .isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughValue() {
        Supplier<String> guard = TypedGuard.create(String.class).build()
                .adaptSupplier(this::returnValue);

        assertThat(guard.get()).isEqualTo("value");
    }

    @Test
    public void supplierPassthroughRuntimeException() {
        Supplier<String> guard = TypedGuard.create(String.class).build()
                .adaptSupplier(this::throwRuntimeException);

        assertThatCode(guard::get)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    private String returnValue() {
        return "value";
    }

    private String throwException() throws TestException {
        throw new TestException();
    }

    private String throwRuntimeException() {
        throw new RuntimeException(new TestException());
    }
}
