package io.smallrye.faulttolerance.core;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Consumer;

/**
 * An append-only, lock-free list of event handlers, in registration order.
 * <p>
 * Handlers are registered on every guarded invocation, so the cost of registration matters.
 * In this implementation, one handler means one object allocation (the list node).
 * Firing an event scans the list linearly, which is fine, as one invocation only ever has
 * a small number of handlers.
 * <p>
 * An event invokes all matching handlers that were registered before the event was fired,
 * in registration order. A handler registered from within a handler of a fired event,
 * on the same thread, is invoked during that fired event, like any other matching handler,
 * after all handlers registered earlier. A handler registered concurrently from another thread
 * while an event is being fired may or may not be invoked; it is invoked by all events fired
 * after the registration has completed.
 * <p>
 * Thread safety:
 * <ul>
 * <li>nodes are immutable, except for the {@code next} reference, which is {@code volatile}
 * and only ever changes once, from {@code null} to the next node</li>
 * <li>handlers are only appended, never removed, so a traversal that has seen a node
 * has also seen all nodes before it</li>
 * <li>{@code tail} is only a hint that makes appending O(1) in the common case; it may lag behind the real
 * tail, which only makes the next append walk a few more nodes</li>
 * </ul>
 */
final class EventHandlerList {
    private static final VarHandle HEAD = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(),
            "head", VarHandle.class, EventHandlerList.class, Node.class);
    private static final VarHandle NEXT = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(),
            "next", VarHandle.class, Node.class, Node.class);

    private static final class Node {
        final Class<? extends FaultToleranceEvent> eventType;
        final Consumer<? extends FaultToleranceEvent> handler;

        volatile Node next;

        Node(Class<? extends FaultToleranceEvent> eventType, Consumer<? extends FaultToleranceEvent> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }
    }

    private volatile Node head; // assigned via CAS through `HEAD`
    private volatile Node tail; // only a hint, may be stale

    <E extends FaultToleranceEvent> void add(Class<E> eventType, Consumer<E> handler) {
        Node node = new Node(eventType, handler);

        Node current = tail;
        if (current == null) {
            if (HEAD.compareAndSet(this, null, node)) {
                tail = node;
                return;
            }
            // lost the race for the first node, which means `head` is no longer `null`
            current = head;
        }

        while (true) {
            Node next = current.next;
            if (next == null) {
                if (NEXT.compareAndSet(current, null, node)) {
                    tail = node;
                    return;
                }
                // lost the race, `current.next` is no longer `null`, re-read it
            } else {
                current = next;
            }
        }
    }

    <E extends FaultToleranceEvent> void fire(E event) {
        Class<? extends FaultToleranceEvent> eventType = event.getClass();
        for (Node node = head; node != null; node = node.next) {
            if (node.eventType == eventType) {
                @SuppressWarnings("unchecked")
                Consumer<E> consumer = (Consumer<E>) node.handler;
                consumer.accept(event);
            }
        }
    }
}
