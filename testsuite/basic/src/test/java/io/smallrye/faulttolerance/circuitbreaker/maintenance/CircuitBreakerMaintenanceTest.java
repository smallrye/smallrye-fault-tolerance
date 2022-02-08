package io.smallrye.faulttolerance.circuitbreaker.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

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
    public void readAndObserveCircuitBreakerState() throws Exception {
        AtomicInteger stateChanges = new AtomicInteger();
        cb.onStateChange("hello", ignored -> {
            stateChanges.incrementAndGet();
        });

        testCircuitBreaker(() -> {
            await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                    .ignoreException(CircuitBreakerOpenException.class)
                    .untilAsserted(() -> {
                        assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
                    });
        });

        // 1. closed -> open
        // 2. open -> half-open
        // 3. half-open -> closed
        assertThat(stateChanges).hasValue(3);
    }

    @Test
    public void resetCircuitBreaker() throws Exception {
        testCircuitBreaker(() -> {
            cb.reset("hello");
        });
    }

    @Test
    public void resetAllCircuitBreakers() throws Exception {
        testCircuitBreaker(() -> {
            cb.resetAll();
        });
    }

    private void testCircuitBreaker(Runnable resetFunction) throws Exception {
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
