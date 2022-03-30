package io.smallrye.faulttolerance.kotlin.reuse

import io.smallrye.common.annotation.Identifier
import io.smallrye.faulttolerance.api.FaultTolerance
import java.util.function.Supplier
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces

@ApplicationScoped
object MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    val FT = FaultTolerance.create<String>()
            .withRetry().maxRetries(2).done()
            .withFallback().handler(Supplier { "fallback" }).done()
            .build()
}
