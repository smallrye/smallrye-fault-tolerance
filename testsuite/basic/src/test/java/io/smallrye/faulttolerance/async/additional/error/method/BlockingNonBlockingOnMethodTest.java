package io.smallrye.faulttolerance.async.additional.error.method;

import javax.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class BlockingNonBlockingOnMethodTest {
    @Test
    public void test(BlockingNonBlockingOnMethodService ignored) {
    }
}
