package io.smallrye.faulttolerance.kotlin.reuse.fallback.guard

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.faulttolerance.Fallback
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
open class MyService {
    companion object {
        val COUNTER = AtomicInteger(0)
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "fallback")
    open fun hello(): String {
        COUNTER.incrementAndGet()
        throw IllegalArgumentException()
    }

    private fun fallback() = "better fallback"
}
