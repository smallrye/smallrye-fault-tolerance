package io.smallrye.faulttolerance.retry.backoff.fibonacci.error;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class NegativeMaxDelayTest {
    @Test
    public void test(NegativeMaxDelayService ignored) {
    }
}
