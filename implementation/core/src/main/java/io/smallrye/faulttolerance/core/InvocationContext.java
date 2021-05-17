package io.smallrye.faulttolerance.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class InvocationContext<V> implements Callable<V> {
    private final Callable<V> delegate;

    public InvocationContext(Callable<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public V call() throws Exception {
        return delegate.call();
    }

    // arbitrary contextual data

    private final ConcurrentMap<Class<?>, Object> data = new ConcurrentHashMap<>();

    public <T> void set(Class<T> clazz, T object) {
        data.put(clazz, object);
    }

    public <T> void remove(Class<T> clazz) {
        data.remove(clazz);
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(data.get(clazz));
    }

    public <T> T get(Class<T> clazz, T defaultValue) {
        T value = get(clazz);
        return value != null ? value : defaultValue;
    }

    // out-of-band communication between fault tolerance strategies in a single chain

    private final ConcurrentMap<Class<? extends InvocationContextEvent>, Collection<Consumer<? extends InvocationContextEvent>>> eventHandlers = new ConcurrentHashMap<>();

    public <E extends InvocationContextEvent> void registerEventHandler(Class<E> eventType, Consumer<E> handler) {
        eventHandlers.computeIfAbsent(eventType, ignored -> new ConcurrentLinkedQueue<>()).add(handler);
    }

    public <E extends InvocationContextEvent> void fireEvent(E event) {
        eventHandlers.getOrDefault(event.getClass(), Collections.emptySet()).forEach(c -> {
            @SuppressWarnings("unchecked")
            Consumer<E> consumer = (Consumer<E>) c;
            consumer.accept(event);
        });
    }
}
