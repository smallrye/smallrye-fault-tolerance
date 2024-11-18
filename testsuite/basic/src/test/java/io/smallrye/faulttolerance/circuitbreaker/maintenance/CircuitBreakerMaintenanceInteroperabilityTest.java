package io.smallrye.faulttolerance.circuitbreaker.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
// Weld SE by default enables relaxed client proxy instantiation, which is different from
// all other environments and pretty crucial for this test
@WithSystemProperty(key = "org.jboss.weld.construction.relaxed", value = "false")
public class CircuitBreakerMaintenanceInteroperabilityTest {
    @Inject
    private HelloService helloService;

    @Inject
    private CircuitBreakerMaintenance cb;

    @BeforeEach
    public void reset() {
        CircuitBreakerMaintenance.get().resetAll();

        helloService.toString(); // force bean instantiation
    }

    @Test
    public void test() {
        CircuitBreakerMaintenance cbm = CircuitBreakerMaintenance.get();

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.CLOSED);

        AtomicInteger helloStateChanges = new AtomicInteger();
        AtomicInteger anotherHelloStateChanges = new AtomicInteger();

        cbm.onStateChange("hello", ignored -> {
            helloStateChanges.incrementAndGet();
        });
        cb.onStateChange("another-hello", ignored -> {
            anotherHelloStateChanges.incrementAndGet();
        });

        for (int i = 0; i < HelloService.THRESHOLD; i++) {
            assertThatThrownBy(() -> {
                helloService.hello(new IOException());
            }).isExactlyInstanceOf(IOException.class);

            assertThatThrownBy(() -> {
                helloService.anotherHello();
            }).isExactlyInstanceOf(RuntimeException.class);
        }

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);

        // hello 1. closed -> open
        assertThat(helloStateChanges).hasValue(1);
        // another-hello 1. closed -> open
        assertThat(anotherHelloStateChanges).hasValue(1);

        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThat(helloService.hello(null)).isEqualTo(HelloService.OK);
                });
        await().atMost(HelloService.DELAY * 2, TimeUnit.MILLISECONDS)
                .ignoreException(CircuitBreakerOpenException.class)
                .untilAsserted(() -> {
                    assertThatThrownBy(helloService::anotherHello).isExactlyInstanceOf(RuntimeException.class);
                });

        assertThat(cb.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cb.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(cbm.currentState("hello")).isEqualTo(CircuitBreakerState.CLOSED);
        assertThat(cbm.currentState("another-hello")).isEqualTo(CircuitBreakerState.OPEN);

        // hello 2. open -> half-open
        // hello 3. half-open -> closed
        assertThat(helloStateChanges).hasValue(3);
        // another-hello 2. open -> half-open
        // another-hello 3. half-open -> open
        assertThat(anotherHelloStateChanges).hasValue(3);
    }
}
