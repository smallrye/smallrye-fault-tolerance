package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneCircuitBreakerTest {
    @BeforeEach
    public void setUp() {
        FaultTolerance.circuitBreakerMaintenance().resetAll();
    }

    @Test
    public void circuitBreaker() throws Exception {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }

        assertThat(guarded.call()).isEqualTo("fallback");
    }

    @Test
    public void circuitBreakerWithSkipOn() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).skipOn(TestException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithFailOn() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).failOn(RuntimeException.class).done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void circuitBreakerWithWhen() {
        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).when(e -> e instanceof RuntimeException).done()
                .withFallback().handler(this::fallback).when(e -> e instanceof CircuitBreakerOpenException).done()
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }

        assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
