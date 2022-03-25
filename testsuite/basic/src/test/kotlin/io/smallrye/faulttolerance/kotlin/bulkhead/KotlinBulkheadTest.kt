package io.smallrye.faulttolerance.kotlin.bulkhead

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.SetSystemProperty

// so that FT methods don't have to be marked @NonBlocking
@SetSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
@FaultToleranceBasicTest
class KotlinBulkheadTest {
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

        // 10 immediately accepted into bulkhead + 10 queued
        assertThat(helloResults).isEqualTo(20)
        assertThat(MyService.helloCounter).hasValue(20)

        assertThat(fallbackResults).isEqualTo(tasks - 20)
        assertThat(MyService.fallbackCounter).hasValue(tasks - 20)
    }
}
