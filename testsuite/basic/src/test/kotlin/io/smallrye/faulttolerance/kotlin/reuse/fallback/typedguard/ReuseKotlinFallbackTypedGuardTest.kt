package io.smallrye.faulttolerance.kotlin.reuse.fallback.typedguard

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jboss.weld.junit5.auto.AddBeanClasses
import org.junit.jupiter.api.Test

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance::class)
class ReuseKotlinFallbackTypedGuardTest {
    @Test
    fun test(service: MyService) = runBlocking<Unit> {
        assertThat(service.hello()).isEqualTo("better fallback")
        assertThat(MyService.COUNTER).hasValue(3)
    }
}
