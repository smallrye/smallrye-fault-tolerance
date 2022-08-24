package io.smallrye.faulttolerance.async.types.rxjava.resubscription;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@ApplicationScoped
public class HelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @AsynchronousNonBlocking
    @Retry
    public Maybe<String> hello() {
        COUNTER.incrementAndGet();
        return Maybe.error(IllegalArgumentException::new);
    }
}
