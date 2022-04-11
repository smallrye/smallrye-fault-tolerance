package io.smallrye.faulttolerance.kotlin.circuitbreaker

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import io.smallrye.faulttolerance.util.WithSystemProperty
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException
import org.junit.jupiter.api.Test
import java.io.IOException

// so that FT methods don't have to be marked @NonBlocking
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
class KotlinCircuitBreakerTest {
    @Test
    fun test(service: MyService) = runBlocking<Unit> {
        for (i in 1..MyService.threshold) {
            try {
                service.hello(true)
            } catch (e: Exception) {
                assertThat(e).isExactlyInstanceOf(IOException::class.java)
            }
        }

        try {
            service.hello(false)
        } catch (e: Exception) {
            assertThat(e).isExactlyInstanceOf(CircuitBreakerOpenException::class.java)
        }

        assertThat(MyService.counter.get()).isEqualTo(MyService.threshold)

        delay(2 * MyService.delay)

        assertThat(service.hello(false)).isEqualTo("hello")
    }
}
