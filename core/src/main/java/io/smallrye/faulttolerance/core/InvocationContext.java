package io.smallrye.faulttolerance.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public final class InvocationContext<V> implements Callable<V> {
    private final Callable<V> delegate;

    public InvocationContext(Callable<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V call() throws Exception {
        return delegate.call();
    }

    // out-of-band communication between fault tolerance strategies in a single chain
    // TODO this needs a bit of a cleanup (perhaps move to a separate package?
    //  or at least move the enum one level up?)

    public enum Event {
        CANCEL,
        TIMEOUT,
    }

    private final ConcurrentMap<Event, Collection<Runnable>> eventHandlers = new ConcurrentHashMap<>();

    public void registerEventHandler(Event event, Runnable handler) {
        eventHandlers.computeIfAbsent(event, ignored -> new ConcurrentLinkedQueue<>()).add(handler);
    }

    public void fireEvent(Event event) {
        eventHandlers.getOrDefault(event, Collections.emptySet()).forEach(Runnable::run);
    }
}
