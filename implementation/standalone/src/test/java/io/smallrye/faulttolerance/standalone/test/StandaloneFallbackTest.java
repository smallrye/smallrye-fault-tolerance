package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneFallbackTest {
    @Test
    public void fallbackWithSupplier() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withFallback().handler(this::fallback).done()
                .build();

        assertThat(guarded.call()).isEqualTo("fallback");
    }

    @Test
    public void fallbackWithFunction() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withFallback().handler(e -> e.getClass().getSimpleName()).done()
                .build();

        assertThat(guarded.call()).isEqualTo("TestException");
    }

    @Test
    public void fallbackWithSkipOn() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withFallback().handler(this::fallback).skipOn(TestException.class).done()
                .build();

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithApplyOn() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withFallback().handler(this::fallback).applyOn(RuntimeException.class).done()
                .build();

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void fallbackWithWhen() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withFallback().handler(this::fallback).when(e -> e instanceof RuntimeException).done()
                .build();

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
