package io.smallrye.faulttolerance.async.types.mutiny.resubscription;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class HelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @AsynchronousNonBlocking
    @Retry
    public Uni<String> hello() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }
}
