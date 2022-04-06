package io.smallrye.faulttolerance.kotlin.timeout

import kotlinx.coroutines.delay
import org.eclipse.microprofile.faulttolerance.ExecutionContext
import org.eclipse.microprofile.faulttolerance.Fallback
import org.eclipse.microprofile.faulttolerance.FallbackHandler
import org.eclipse.microprofile.faulttolerance.Timeout
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    @Timeout(500)
    @Fallback(MyFallbackHandler::class)
    open suspend fun hello(): Int {
        delay(2000)
        return 13
    }

    class MyFallbackHandler : FallbackHandler<Int> {
        override fun handle(context: ExecutionContext) = 42
    }
}
