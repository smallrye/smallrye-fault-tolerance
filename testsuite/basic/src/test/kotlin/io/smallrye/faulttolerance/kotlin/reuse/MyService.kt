package io.smallrye.faulttolerance.kotlin.reuse

import io.smallrye.faulttolerance.api.ApplyFaultTolerance
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicInteger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class MyService {
    companion object {
        val COUNTER = AtomicInteger(0)
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    open fun hello(): String {
        COUNTER.incrementAndGet()
        throw IllegalArgumentException()
    }
}
