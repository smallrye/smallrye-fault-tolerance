package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.TestException;

public class StandaloneCircuitBreakerMaintenanceTest {
    @BeforeEach
    public void setUp() {
        FaultTolerance.circuitBreakerMaintenance().resetAll();
    }

    @Test
    public void circuitBreakerEvents() throws Exception {
        assertThatThrownBy(() -> {
            FaultTolerance.circuitBreakerMaintenance().currentState("my-cb");
        });

        Callable<String> guarded = FaultTolerance.createCallable(this::action)
                .withCircuitBreaker().requestVolumeThreshold(4).delay(1, ChronoUnit.SECONDS).name("my-cb").done()
                .withFallback().handler(this::fallback).applyOn(CircuitBreakerOpenException.class).done()
                .build();

        AtomicInteger stateChanges = new AtomicInteger();
        FaultTolerance.circuitBreakerMaintenance().onStateChange("my-cb", ignored -> stateChanges.incrementAndGet());

        assertThat(stateChanges).hasValue(0);

        for (int i = 0; i < 4; i++) {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        }

        // 1. closed -> open
        assertThat(stateChanges).hasValue(1);

        assertThat(guarded.call()).isEqualTo("fallback");

        await().untilAsserted(() -> {
            assertThatCode(guarded::call).isExactlyInstanceOf(TestException.class);
        });

        // 2. open -> half-open
        // 3. half-open -> open
        assertThat(stateChanges).hasValue(3);
    }

    public String action() throws TestException {
        throw new TestException();
    }

    public String fallback() {
        return "fallback";
    }
}
