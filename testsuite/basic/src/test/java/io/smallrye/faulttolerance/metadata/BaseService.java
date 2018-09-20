package io.smallrye.faulttolerance.metadata;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Retry;

@Retry
public class BaseService {

    static final AtomicInteger COUNTER = new AtomicInteger(0);

    public String retry() {
        if (COUNTER.incrementAndGet() == 5) {
            return "ok";
        }
        throw new IllegalStateException();
    }
}
