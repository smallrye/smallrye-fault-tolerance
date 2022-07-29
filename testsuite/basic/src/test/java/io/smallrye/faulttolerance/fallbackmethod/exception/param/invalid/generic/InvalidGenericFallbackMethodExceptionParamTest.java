package io.smallrye.faulttolerance.fallbackmethod.exception.param.invalid.generic;

import javax.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
public class InvalidGenericFallbackMethodExceptionParamTest {
    @Test
    public void test(InvalidGenericService ignored) {
    }
}
