package io.smallrye.faulttolerance.kotlin.reuse.fallback.typedguard

import io.smallrye.common.annotation.Identifier
import io.smallrye.faulttolerance.api.TypedGuard
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import java.util.function.Supplier

@ApplicationScoped
object MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    val GUARD = TypedGuard.create(String::class.java)
        .withRetry().maxRetries(2).done()
        .withFallback().handler(Supplier { "fallback" }).done()
        .build()
}
