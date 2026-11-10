package io.smallrye.faulttolerance.retry.computed.delay.error;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class RetryOnClassComputedDelayOnMethodTest {
    @Test
    public void test(RetryOnClassComputedDelayOnMethodService ignored) {
    }
}
