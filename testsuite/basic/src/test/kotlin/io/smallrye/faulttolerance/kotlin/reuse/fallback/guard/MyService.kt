package io.smallrye.faulttolerance.kotlin.reuse.fallback.guard

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Fallback
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
open class MyService {
    companion object {
        val COUNTER = AtomicInteger(0)
    }

    @ApplyGuard("my-fault-tolerance")
    @Fallback(fallbackMethod = "fallback")
    open suspend fun hello(): String {
        COUNTER.incrementAndGet()
        delay(100)
        throw IllegalArgumentException()
    }

    private suspend fun fallback(): String {
        delay(100)
        return "better fallback"
    }
}
