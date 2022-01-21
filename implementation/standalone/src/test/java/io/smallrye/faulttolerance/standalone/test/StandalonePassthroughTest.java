package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandalonePassthroughTest {
    @Test
    public void passthroughValue() throws Exception {
        FaultTolerance<String> guard = FaultTolerance.<String> create().build();

        assertThat(guard.call(this::returnValue)).isEqualTo("value");
        assertThat(guard.get(this::returnValue)).isEqualTo("value");
        assertThatCode(() -> guard.run(this::returnValue)).doesNotThrowAnyException();
    }

    @Test
    public void passthroughRuntimeException() {
        FaultTolerance<Object> guard = FaultTolerance.create().build();

        assertThatCode(() -> guard.call(this::throwRuntimeException))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);

        assertThatCode(() -> guard.get(this::throwRuntimeException))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);

        assertThatCode(() -> guard.run(this::throwRuntimeException))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void passthroughException() {
        FaultTolerance<Object> guard = FaultTolerance.create().build();

        assertThatCode(() -> guard.call(this::throwException))
                .isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughValue() throws Exception {
        Callable<String> guard = FaultTolerance.createCallable(this::returnValue).build();

        assertThat(guard.call()).isEqualTo("value");
    }

    @Test
    public void callablePassthroughRuntimeException() {
        Callable<String> guard = FaultTolerance.createCallable(this::throwRuntimeException).build();

        assertThatCode(guard::call)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void callablePassthroughException() {
        Callable<String> guard = FaultTolerance.createCallable(this::throwException).build();

        assertThatCode(guard::call)
                .isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void supplierPassthroughValue() {
        Supplier<String> guard = FaultTolerance.createSupplier(this::returnValue).build();

        assertThat(guard.get()).isEqualTo("value");
    }

    @Test
    public void supplierPassthroughRuntimeException() {
        Supplier<String> guard = FaultTolerance.createSupplier(this::throwRuntimeException).build();

        assertThatCode(guard::get)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(TestException.class);
    }

    @Test
    public void runnablePassthroughValue() {
        Runnable guard = FaultTolerance.createRunnable(this::returnValue).build();

        assertThatCode(guard::run).doesNotThrowAnyException();
    }

    @Test
    public void runnablePassthroughRuntimeException() {
        Runnable guard = FaultTolerance.createRunnable(this::throwRuntimeException).build();

        assertThatCode(guard::run)
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
