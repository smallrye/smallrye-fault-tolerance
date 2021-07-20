package io.smallrye.faulttolerance.circuitbreaker.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerMaintenanceTest {
    @Inject
    private HelloService helloService;

    @Inject
    private CircuitBreakerMaintenance cb;

    @BeforeEach
    public void reset() {
        cb.resetAll();
    }

    @Test
    public void readCircuitBreakerState() {
        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThatThrownBy(() -> {
                helloService.hello(new IOException());
            }).isExactlyInstanceOf(IOException.class);
        }

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);

        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
                });

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
    }

    @Test
    public void resetCircuitBreaker() throws Exception {
        testCircuitBreakerReset(() -> {
            cb.reset("hello");
        });
    }

    @Test
    public void resetAllCircuitBreakers() throws Exception {
        testCircuitBreakerReset(() -> {
            cb.resetAll();
        });
    }

    private void testCircuitBreakerReset(Runnable resetFunction) throws Exception {
        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThatThrownBy(() -> {
                helloService.hello(new IOException());
            }).isExactlyInstanceOf(IOException.class);
        }

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);

        assertThatThrownBy(() -> {
            helloService.hello(null);
        }).isExactlyInstanceOf(CircuitBreakerOpenException.class);

        resetFunction.run();

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);

        assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
    }

    @Test
    public void undefinedCircuitBreaker() {
        assertThatThrownBy(() -> {
            cb.currentState("undefined");
        }).isExactlyInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> {
            cb.reset("undefined");
        }).isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
