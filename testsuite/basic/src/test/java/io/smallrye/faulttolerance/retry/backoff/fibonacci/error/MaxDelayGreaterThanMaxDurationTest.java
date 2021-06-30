package io.smallrye.faulttolerance.retry.backoff.fibonacci.error;

import javax.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class MaxDelayGreaterThanMaxDurationTest {
    @Test
    public void test(MaxDelayGreaterThanMaxDurationService ignored) {
    }
}
