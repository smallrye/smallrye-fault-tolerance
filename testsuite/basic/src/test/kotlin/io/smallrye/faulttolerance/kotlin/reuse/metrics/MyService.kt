package io.smallrye.faulttolerance.kotlin.reuse.metrics

import io.smallrye.faulttolerance.api.ApplyGuard
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    @ApplyGuard("my-fault-tolerance")
    open fun first(): String {
        throw IllegalArgumentException()
    }

    @ApplyGuard("my-fault-tolerance")
    open fun second(): String {
        throw IllegalStateException()
    }
}
