package io.smallrye.faulttolerance.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class EventHandlerListTest {
    enum EventA implements FaultToleranceEvent {
        INSTANCE
    }

    enum EventB implements FaultToleranceEvent {
        INSTANCE
    }

    @Test
    public void firingEmptyList() {
        EventHandlerList list = new EventHandlerList();

        list.fire(EventA.INSTANCE); // must not fail
    }

    @Test
    public void handlerIsInvokedWithTheFiredEvent() {
        EventHandlerList list = new EventHandlerList();

        List<FaultToleranceEvent> received = new ArrayList<>();
        list.add(EventA.class, received::add);

        list.fire(EventA.INSTANCE);

        assertThat(received).containsExactly(EventA.INSTANCE);
    }

    @Test
    public void onlyHandlersOfTheFiredEventTypeAreInvoked() {
        EventHandlerList list = new EventHandlerList();

        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        list.add(EventA.class, ignored -> a.incrementAndGet());
        list.add(EventB.class, ignored -> b.incrementAndGet());

        list.fire(EventA.INSTANCE);
        assertThat(a).hasValue(1);
        assertThat(b).hasValue(0);

        list.fire(EventB.INSTANCE);
        list.fire(EventB.INSTANCE);
        assertThat(a).hasValue(1);
        assertThat(b).hasValue(2);
    }

    @Test
    public void handlersAreInvokedInRegistrationOrder() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        list.add(EventA.class, ignored -> invoked.add("a1"));
        list.add(EventB.class, ignored -> invoked.add("b1"));
        list.add(EventA.class, ignored -> invoked.add("a2"));
        list.add(EventB.class, ignored -> invoked.add("b2"));
        list.add(EventA.class, ignored -> invoked.add("a3"));

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("a1", "a2", "a3");

        invoked.clear();
        list.fire(EventB.INSTANCE);
        assertThat(invoked).containsExactly("b1", "b2");
    }

    @Test
    public void handlersCanBeInvokedRepeatedly() {
        EventHandlerList list = new EventHandlerList();

        AtomicInteger counter = new AtomicInteger();
        list.add(EventA.class, ignored -> counter.incrementAndGet());

        for (int i = 0; i < 5; i++) {
            list.fire(EventA.INSTANCE);
        }

        assertThat(counter).hasValue(5);
    }

    @Test
    public void handlersCanBeRegisteredAfterFiring() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        list.add(EventA.class, ignored -> invoked.add("first"));
        list.fire(EventA.INSTANCE);

        list.add(EventA.class, ignored -> invoked.add("second"));
        list.fire(EventA.INSTANCE);

        assertThat(invoked).containsExactly("first", "first", "second");
    }

    @Test
    public void handlerRegisteredWhileFiringIsInvoked_registeringHandlerIsLast() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        AtomicBoolean registered = new AtomicBoolean();
        list.add(EventA.class, ignored -> {
            invoked.add("outer");
            if (registered.compareAndSet(false, true)) {
                list.add(EventA.class, ignored2 -> invoked.add("inner"));
            }
        });

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer", "inner");

        invoked.clear();
        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer", "inner");
    }

    @Test
    public void handlerRegisteredWhileFiringIsInvoked_registeringHandlerIsNotLast() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        AtomicBoolean registered = new AtomicBoolean();
        list.add(EventA.class, ignored -> {
            invoked.add("outer");
            if (registered.compareAndSet(false, true)) {
                list.add(EventA.class, ignored2 -> invoked.add("inner"));
            }
        });
        list.add(EventA.class, ignored -> invoked.add("other"));

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer", "other", "inner");

        invoked.clear();
        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer", "other", "inner");
    }

    @Test
    public void handlerRegisteredWhileFiringIsInvoked_afterTrailingHandlersOfOtherTypes() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        AtomicBoolean registered = new AtomicBoolean();
        list.add(EventA.class, ignored -> {
            invoked.add("outer");
            if (registered.compareAndSet(false, true)) {
                list.add(EventA.class, ignored2 -> invoked.add("inner"));
            }
        });
        list.add(EventB.class, ignored -> invoked.add("unrelated"));

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer", "inner");
    }

    @Test
    public void handlerRegisteredWhileFiringIsNotInvokedForOtherEventTypes() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        AtomicBoolean registered = new AtomicBoolean();
        list.add(EventA.class, ignored -> {
            invoked.add("outer");
            if (registered.compareAndSet(false, true)) {
                list.add(EventB.class, ignored2 -> invoked.add("inner"));
            }
        });

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("outer");

        invoked.clear();
        list.fire(EventB.INSTANCE);
        assertThat(invoked).containsExactly("inner");
    }

    @Test
    public void handlerRegisteredWhileFiringCanRegisterFurtherHandlers() {
        EventHandlerList list = new EventHandlerList();

        List<String> invoked = new ArrayList<>();
        list.add(EventA.class, ignored -> {
            invoked.add("first");
            list.add(EventA.class, ignored2 -> {
                invoked.add("second");
                if (invoked.size() < 3) {
                    list.add(EventA.class, ignored3 -> invoked.add("third"));
                }
            });
        });

        list.fire(EventA.INSTANCE);
        assertThat(invoked).containsExactly("first", "second", "third");
    }

    @Test
    public void exceptionFromHandlerPropagates() {
        EventHandlerList list = new EventHandlerList();

        AtomicInteger counter = new AtomicInteger();
        list.add(EventA.class, ignored -> {
            throw new IllegalStateException("expected");
        });
        list.add(EventA.class, ignored -> counter.incrementAndGet());

        assertThatThrownBy(() -> list.fire(EventA.INSTANCE))
                .isExactlyInstanceOf(IllegalStateException.class);
        // the exception stops the dispatch
        assertThat(counter).hasValue(0);
    }

    @Test
    public void concurrentRegistrationLosesNoHandlers() throws Exception {
        int threads = 8;
        int handlersPerThread = 2_000;

        EventHandlerList list = new EventHandlerList();
        CountDownLatch start = new CountDownLatch(1);
        List<int[]> invokedA = Collections.synchronizedList(new ArrayList<>());
        List<int[]> invokedB = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int thread = t;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < handlersPerThread; i++) {
                        int seq = i;
                        if (seq % 2 == 0) {
                            list.add(EventA.class, ignored -> invokedA.add(new int[] { thread, seq }));
                        } else {
                            list.add(EventB.class, ignored -> invokedB.add(new int[] { thread, seq }));
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        list.fire(EventA.INSTANCE);
        list.fire(EventB.INSTANCE);

        assertThat(invokedA).hasSize(threads * handlersPerThread / 2);
        assertThat(invokedB).hasSize(threads * handlersPerThread / 2);

        // handlers registered by a single thread must be invoked in the order that thread registered them
        for (List<int[]> invoked : List.of(invokedA, invokedB)) {
            int[] lastSeen = new int[threads];
            Arrays.fill(lastSeen, -1);
            for (int[] entry : invoked) {
                assertThat(entry[1]).isGreaterThan(lastSeen[entry[0]]);
                lastSeen[entry[0]] = entry[1];
            }
        }
    }

    @Test
    public void concurrentRegistrationAndFiring() throws Exception {
        int registrations = 5_000;

        EventHandlerList list = new EventHandlerList();
        AtomicInteger invocations = new AtomicInteger();
        AtomicBoolean done = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> registrar = executor.submit(() -> {
                start.countDown();
                for (int i = 0; i < registrations; i++) {
                    list.add(EventA.class, ignored -> invocations.incrementAndGet());
                }
                done.set(true);
            });
            Future<?> firer = executor.submit(() -> {
                start.await();
                // must never fail or hang, regardless of how the list grows underneath
                while (!done.get()) {
                    list.fire(EventA.INSTANCE);
                }
                return null;
            });
            registrar.get(30, TimeUnit.SECONDS);
            firer.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // once everything is registered, a single firing invokes every handler exactly once
        invocations.set(0);
        list.fire(EventA.INSTANCE);
        assertThat(invocations).hasValue(registrations);
    }
}
