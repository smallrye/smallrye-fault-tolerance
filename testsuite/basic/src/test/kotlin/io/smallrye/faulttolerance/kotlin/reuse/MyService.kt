package io.smallrye.faulttolerance.kotlin.reuse

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
open class MyService {
    companion object {
        val COUNTER = AtomicInteger(0)
    }

    @ApplyGuard("my-fault-tolerance")
    open fun hello(): String {
        COUNTER.incrementAndGet()
        throw IllegalArgumentException()
    }
}
