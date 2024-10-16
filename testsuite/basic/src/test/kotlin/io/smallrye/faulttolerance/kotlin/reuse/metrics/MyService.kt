package io.smallrye.faulttolerance.kotlin.reuse.metrics

import io.smallrye.faulttolerance.api.ApplyFaultTolerance
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicInteger
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    @ApplyFaultTolerance("my-fault-tolerance")
    open fun first(): String {
        throw IllegalArgumentException()
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    open fun second(): String {
        throw IllegalStateException()
    }
}
