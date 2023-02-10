package io.smallrye.faulttolerance.retry.backoff.error;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class TwoBackoffsOnMethodTest {
    @Test
    public void test(TwoBackoffsOnMethodService ignored) {
    }
}
