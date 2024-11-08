package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneCircuitBreakerEventsTest {
    private boolean actionShouldFail;

    @BeforeEach
    public void setUp() {
        CircuitBreakerMaintenance.get().resetAll();
    }

    @Test
    public void circuitBreakerEvents() throws Exception {
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();
        AtomicInteger preventedCounter = new AtomicInteger();
        AtomicReference<CircuitBreakerState> changedState = new AtomicReference<>();

        Callable<String> guarded = TypedGuard.create(String.class)
                .withCircuitBreaker()
                .requestVolumeThreshold(4)
                .onSuccess(successCounter::incrementAndGet)
                .onFailure(failureCounter::incrementAndGet)
                .onPrevented(preventedCounter::incrementAndGet)
                .onStateChange(changedState::set)
                .done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build()
                .adaptCallable(this::action);

        actionShouldFail = false;
        for (int i = 0; i < 2; i++) {
            assertThat(guarded.call()).isEqualTo("value");
        }
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(0);
        assertThat(preventedCounter).hasValue(0);
        assertThat(changedState).hasValue(null);

        actionShouldFail = true;
        for (int i = 0; i < 2; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(2);
        assertThat(preventedCounter).hasValue(0);
        assertThat(changedState).hasValue(CircuitBreakerState.OPEN);

        assertThat(guarded.call()).isEqualTo("fallback");
        assertThat(successCounter).hasValue(2);
        assertThat(failureCounter).hasValue(2);
        assertThat(preventedCounter).hasValue(1);
    }

    public String action() throws TestException {
        if (actionShouldFail) {
            throw new TestException();
        } else {
            return "value";
        }
    }

    public String fallback() {
        return "fallback";
    }
}
