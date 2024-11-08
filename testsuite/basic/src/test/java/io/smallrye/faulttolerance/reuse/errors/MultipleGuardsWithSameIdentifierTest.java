package io.smallrye.faulttolerance.reuse.errors;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.ExpectedDeploymentException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException.class)
public class MultipleGuardsWithSameIdentifierTest {
    @Test
    public void test(MultipleGuardsWithSameIdentifierProducer ignored) {
    }
}
