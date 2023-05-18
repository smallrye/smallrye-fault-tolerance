package io.smallrye.faulttolerance.kotlin.fallback.invalid

import io.smallrye.faulttolerance.util.ExpectedDeploymentException
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import io.smallrye.faulttolerance.util.WithSystemProperty
import jakarta.enterprise.inject.spi.DefinitionException
import org.junit.jupiter.api.Test

// so that FT methods don't have to be marked @AsynchronousNonBlocking
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
@ExpectedDeploymentException(DefinitionException::class)
class KotlinInvalidFallbackTest {
    @Test
    fun test(ignored: MyService) {
    }
}
