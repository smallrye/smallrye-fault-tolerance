package io.smallrye.faulttolerance.retry.when.error;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class RetryOnAndRetryWhenExceptionTest {
    @Test
    public void test(RetryOnAndRetryWhenExceptionService ignored) {
    }
}
