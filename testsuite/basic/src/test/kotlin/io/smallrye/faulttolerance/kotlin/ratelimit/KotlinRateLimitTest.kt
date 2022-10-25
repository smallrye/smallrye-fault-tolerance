package io.smallrye.faulttolerance.kotlin.ratelimit

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import io.smallrye.faulttolerance.util.WithSystemProperty
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// so that FT methods don't have to be marked @AsynchronousNonBlocking
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
class KotlinRateLimitTest {
    private val tasks = 20_000

    @Test
    fun test(service: MyService) = runBlocking<Unit> {
        val results = (1..tasks).map {
            async {
                service.hello()
            }
        }.map {
            it.await()
        }

        val helloResults = results.count { it == "hello" }
        val fallbackResults = results.count { it == "fallback" }

        // 1000 permitted
        assertThat(helloResults).isEqualTo(1000)
        assertThat(MyService.helloCounter).hasValue(1000)

        assertThat(fallbackResults).isEqualTo(tasks - 1000)
        assertThat(MyService.fallbackCounter).hasValue(tasks - 1000)
    }
}
