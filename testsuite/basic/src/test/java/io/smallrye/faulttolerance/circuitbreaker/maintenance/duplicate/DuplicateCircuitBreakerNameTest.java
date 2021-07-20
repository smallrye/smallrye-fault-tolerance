package io.smallrye.faulttolerance.circuitbreaker.maintenance.duplicate;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class DuplicateCircuitBreakerNameTest {
    @Test
    public void ignored(CircuitBreakerService1 ignored1, CircuitBreakerService2 ignored2) {
    }
}
