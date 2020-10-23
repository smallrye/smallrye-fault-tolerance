package io.smallrye.faulttolerance.circuitbreaker.maintenance.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerNameInheritanceTest {
    @Inject
    private SuperCircuitBreakerService superCircuitBreakerService;

    @Inject
    private SubCircuitBreakerService subCircuitBreakerService;

    @Inject
    private CircuitBreakerMaintenance circuitBreakerMaintenance;

    @Test
    public void deploysWithoutError() {
        assertThat(superCircuitBreakerService).isNotNull();
        assertThat(subCircuitBreakerService).isNotNull();
        assertThat(circuitBreakerMaintenance).isNotNull();
    }
}
