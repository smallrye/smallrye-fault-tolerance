package io.smallrye.faulttolerance.reuse.mixed.bulkhead;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@ApplicationScoped
public class MyFaultTolerance {
    @Produces
    @Identifier("my-fault-tolerance")
    public static final Guard GUARD = Guard.create()
            .withBulkhead().limit(3).queueSize(2).done()
            .build();
}
