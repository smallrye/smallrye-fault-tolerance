package io.smallrye.faulttolerance.kotlin.fallback

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.Fallback

@ApplicationScoped
open class MyService {
    @Fallback(fallbackMethod = "fallback")
    open suspend fun hello(name: String): String {
        delay(1000)
        throw IllegalArgumentException()
    }

    open suspend fun fallback(name: String, ex: Exception): String {
        delay(1000)
        return "Hello, ${name.uppercase()}"
    }
}
