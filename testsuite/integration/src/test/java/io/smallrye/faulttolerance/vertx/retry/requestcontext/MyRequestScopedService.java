package io.smallrye.faulttolerance.vertx.retry.requestcontext;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class MyRequestScopedService {
    static final Queue<Integer> instanceIds = new ConcurrentLinkedQueue<>();

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final Integer id = counter.incrementAndGet();

    public void call() {
        instanceIds.add(id);
    }
}
