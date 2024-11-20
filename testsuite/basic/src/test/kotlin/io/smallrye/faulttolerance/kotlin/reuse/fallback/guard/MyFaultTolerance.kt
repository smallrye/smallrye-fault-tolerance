package io.smallrye.faulttolerance.kotlin.reuse.fallback.guard

import io.smallrye.common.annotation.Identifier
import io.smallrye.faulttolerance.api.Guard
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces

@ApplicationScoped
object MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    val GUARD = Guard.create()
        .withRetry().maxRetries(2).done()
        .build()
}
