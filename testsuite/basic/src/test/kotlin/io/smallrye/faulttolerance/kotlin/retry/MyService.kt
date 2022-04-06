package io.smallrye.faulttolerance.kotlin.retry

import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Fallback
import org.eclipse.microprofile.faulttolerance.Retry
import java.util.concurrent.atomic.AtomicInteger
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    companion object {
        val counter = AtomicInteger(0)
    }

    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "helloFallback")
    open suspend fun hello(): Int {
        delay(100)
        counter.incrementAndGet()
        delay(100)
        throw IllegalArgumentException()
    }

    private suspend fun helloFallback(): Int {
        delay(100)
        return 42
    }
}
