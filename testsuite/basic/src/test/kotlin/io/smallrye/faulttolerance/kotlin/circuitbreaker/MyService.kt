package io.smallrye.faulttolerance.kotlin.circuitbreaker

import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.CircuitBreaker
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    companion object {
        const val threshold = 10
        const val delay = 500L

        val counter = AtomicInteger(0)
    }

    @CircuitBreaker(requestVolumeThreshold = threshold, failureRatio = 0.5, delay = delay, successThreshold = 1)
    open suspend fun hello(fail: Boolean): String {
        counter.incrementAndGet()
        delay(100)
        if (fail) {
            throw IOException()
        }
        return "hello"
    }
}
