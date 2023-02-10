package io.smallrye.faulttolerance.async.additional.error.clazz;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class BlockingNonBlockingOnClassTest {
    @Test
    public void test(BlockingNonBlockingOnClassService ignored) {
    }
}
