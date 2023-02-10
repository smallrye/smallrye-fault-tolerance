package io.smallrye.faulttolerance.kotlin.ratelimit

import io.smallrye.faulttolerance.api.RateLimit
import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Bulkhead
import org.eclipse.microprofile.faulttolerance.Fallback
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    companion object {
        val helloCounter = AtomicInteger(0)
        val fallbackCounter = AtomicInteger(0)
    }

    @RateLimit(value = 1000, window = 5, windowUnit = ChronoUnit.MINUTES)
    @Fallback(fallbackMethod = "fallback")
    open suspend fun hello(): String {
        helloCounter.incrementAndGet()
        delay(100)
        return "hello"
    }

    private suspend fun fallback(): String {
        fallbackCounter.incrementAndGet()
        delay(100)
        return "fallback"
    }
}
