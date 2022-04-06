package io.smallrye.faulttolerance.kotlin.reuse

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest
import org.assertj.core.api.Assertions.assertThat
import org.jboss.weld.junit5.auto.AddBeanClasses
import org.junit.jupiter.api.Test

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance::class)
class ReuseKotlinTest {
    @Test
    fun test(service: MyService) {
        assertThat(service.hello()).isEqualTo("fallback")
    }
}
