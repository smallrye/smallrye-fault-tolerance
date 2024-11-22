package io.smallrye.faulttolerance.kotlin.reuse.metrics

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay

@ApplicationScoped
open class MyService {
    @ApplyGuard("my-fault-tolerance")
    open suspend fun first(): String {
        delay(100)
        throw IllegalArgumentException()
    }

    @ApplyGuard("my-fault-tolerance")
    open suspend fun second(): String {
        delay(100)
        throw IllegalStateException()
    }
}
