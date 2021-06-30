package io.smallrye.faulttolerance.retry.backoff.exponential.error;

import javax.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class NegativeFactorTest {
    @Test
    public void test(NegativeFactorService ignored) {
    }
}
