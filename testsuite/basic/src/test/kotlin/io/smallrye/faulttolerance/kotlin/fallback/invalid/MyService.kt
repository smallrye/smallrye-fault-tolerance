package io.smallrye.faulttolerance.kotlin.fallback.invalid

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Fallback

@ApplicationScoped
open class MyService {
    @Fallback(fallbackMethod = "fallback")
    open suspend fun hello(name: String): String {
        throw IllegalArgumentException()
    }

    open suspend fun fallback(name: String, ex: Exception): Int {
        return 0
    }
}
