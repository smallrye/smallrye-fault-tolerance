package io.smallrye.faulttolerance.kotlin.bulkhead

import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Bulkhead
import org.eclipse.microprofile.faulttolerance.Fallback
import java.util.concurrent.atomic.AtomicInteger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    companion object {
        val helloCounter = AtomicInteger(0)
        val fallbackCounter = AtomicInteger(0)
    }

    @Bulkhead
    @Fallback(fallbackMethod = "fallback")
    open suspend fun hello(): String {
        helloCounter.incrementAndGet()
        delay(2000)
        return "hello"
    }

    private suspend fun fallback(): String {
        fallbackCounter.incrementAndGet()
        delay(100)
        return "fallback"
    }
}
