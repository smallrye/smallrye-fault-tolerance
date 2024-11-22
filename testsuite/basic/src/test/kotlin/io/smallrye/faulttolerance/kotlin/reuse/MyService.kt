package io.smallrye.faulttolerance.kotlin.reuse

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
open class MyService {
    companion object {
        val COUNTER = AtomicInteger(0)
    }

    @ApplyGuard("my-fault-tolerance")
    open suspend fun hello(): String {
        COUNTER.incrementAndGet()
        delay(100)
        throw IllegalArgumentException()
    }
}
