package io.smallrye.faulttolerance.kotlin.fallback

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import io.smallrye.faulttolerance.util.WithSystemProperty
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// so that FT methods don't have to be marked @AsynchronousNonBlocking
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
class KotlinFallbackTest {
    @Test
    fun test(service: MyService) = runBlocking<Unit> {
        val result = service.hello("world")
        assertThat(result).isEqualTo("Hello, WORLD")
    }
}
