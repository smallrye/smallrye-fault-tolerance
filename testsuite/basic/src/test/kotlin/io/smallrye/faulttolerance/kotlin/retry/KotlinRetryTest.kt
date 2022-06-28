package io.smallrye.faulttolerance.kotlin.retry

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import io.smallrye.faulttolerance.util.WithSystemProperty
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// so that FT methods don't have to be marked @NonBlocking
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
class KotlinRetryTest {
    @Test
    fun test(service: MyService) = runBlocking<Unit> {
        val result = service.hello()
        assertThat(result).isEqualTo(42)
        assertThat(MyService.counter).hasValue(3)
    }
}
