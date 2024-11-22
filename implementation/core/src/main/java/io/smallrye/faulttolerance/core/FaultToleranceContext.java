package io.smallrye.faulttolerance.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FaultToleranceContext<V> {
    private final Supplier<Future<V>> delegate;
    private final boolean isAsync;

    public FaultToleranceContext(Supplier<Future<V>> delegate, boolean isAsync) {
        this.delegate = delegate;
        this.isAsync = isAsync;
    }

    public Future<V> call() {
        return delegate.get();
    }

    /**
     * Whether the guarded operation is truly asynchronous (that is, returns
     * a {@code CompletionStage} of the result, or some other asynchronous type).
     */
    public boolean isAsync() {
        return isAsync;
    }

    /**
     * Whether the guarded operation is synchronous. This includes pseudo-asynchronous
     * operations (that return a {@code Future} of the result).
     */
    public boolean isSync() {
        return !isAsync;
    }

    // arbitrary contextual data

    private final ConcurrentMap<Class<?>, Object> data = new ConcurrentHashMap<>(4);

    public <T> void set(Class<T> clazz, T object) {
        data.put(clazz, object);
    }

    public <T> T remove(Class<T> clazz) {
        return clazz.cast(data.remove(clazz));
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(data.get(clazz));
    }

    public <T> T get(Class<T> clazz, T defaultValue) {
        T value = get(clazz);
        return value != null ? value : defaultValue;
    }

    // out-of-band communication between fault tolerance strategies in a single chain

    private final ConcurrentMap<Class<? extends FaultToleranceEvent>, Collection<Consumer<? extends FaultToleranceEvent>>> eventHandlers = new ConcurrentHashMap<>();

    public <E extends FaultToleranceEvent> void registerEventHandler(Class<E> eventType, Consumer<E> handler) {
        eventHandlers.computeIfAbsent(eventType, ignored -> new ConcurrentLinkedQueue<>()).add(handler);
    }

    public <E extends FaultToleranceEvent> void fireEvent(E event) {
        Collection<Consumer<? extends FaultToleranceEvent>> handlers = eventHandlers.get(event.getClass());
        if (handlers != null) {
            for (Consumer<? extends FaultToleranceEvent> handler : handlers) {
                @SuppressWarnings("unchecked")
                Consumer<E> consumer = (Consumer<E>) handler;
                consumer.accept(event);
            }
        }
    }
}
