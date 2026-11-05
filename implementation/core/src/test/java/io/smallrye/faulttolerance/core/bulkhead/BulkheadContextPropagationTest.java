package io.smallrye.faulttolerance.core.bulkhead;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.sync;
import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Completer;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.util.RunnableWrapper;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.party.Party;

/**
 * An async bulkhead runs a queued task on the thread that completed some previously running
 * task. Each task must still observe the contexts of the invocation that created it; observing
 * the contexts of an unrelated invocation would be a data leak between callers.
 */
public class BulkheadContextPropagationTest {
    /**
     * A stand-in for thread context propagation. The "context" is a single thread local,
     * which {@link #CONTEXT_PROPAGATION} captures when a {@code Runnable} is wrapped
     * and restores while that {@code Runnable} runs.
     */
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private static final RunnableWrapper CONTEXT_PROPAGATION = runnable -> {
        String captured = CONTEXT.get();
        return () -> {
            String previous = CONTEXT.get();
            CONTEXT.set(captured);
            try {
                runnable.run();
            } finally {
                CONTEXT.set(previous);
            }
        };
    };

    private Bulkhead<String> bulkhead(FaultToleranceStrategy<String> delegate, int size, int queueSize,
            boolean syncQueueing) {
        return new Bulkhead<>(delegate, "test", size, queueSize, syncQueueing, CONTEXT_PROPAGATION);
    }

    /**
     * The context observed by each invocation, in invocation order.
     */
    private final List<String> observedContexts = new CopyOnWriteArrayList<>();

    /**
     * Completers of invocations that returned a pending future, in invocation order.
     */
    private final BlockingQueue<Completer<String>> pendingInvocations = new LinkedBlockingQueue<>();

    /**
     * An invocation that never completes on its own; complete it through {@link #nextPendingInvocation()}.
     */
    private final FaultToleranceStrategy<String> pendingInvocation = ctx -> {
        observedContexts.add(CONTEXT.get());
        Completer<String> completer = Completer.create();
        pendingInvocations.add(completer);
        return completer.future();
    };

    /**
     * An invocation that is already complete when it returns; its value is the observed context.
     */
    private final FaultToleranceStrategy<String> completedInvocation = ctx -> {
        String context = CONTEXT.get();
        observedContexts.add(context);
        return Future.of(context);
    };

    /**
     * An invocation holder whose value can be changed dynamically. Initially, it is {@link #pendingInvocation}.
     * Often, it becomes {@link #completedInvocation} afterwards.
     */
    private final AtomicReference<FaultToleranceStrategy<String>> currentInvocationStrategy = new AtomicReference<>(
            pendingInvocation);

    /**
     * An invocation that delegates to the current value of {@link #currentInvocationStrategy}.
     */
    private final FaultToleranceStrategy<String> currentInvocation = ctx -> currentInvocationStrategy.get().apply(ctx);

    @AfterEach
    public void tearDown() {
        CONTEXT.remove();
    }

    private Completer<String> nextPendingInvocation() throws InterruptedException {
        Completer<String> completer = pendingInvocations.poll(5, TimeUnit.SECONDS);
        assertThat(completer).isNotNull();
        return completer;
    }

    /**
     * Runs {@code action} on a new thread whose context is {@code context} and waits for it
     * to finish. Returns the context that the thread observes after the action has finished.
     */
    private String runOnThreadWithContext(String context, Runnable action) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<String> contextAtEnd = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            CONTEXT.set(context);
            try {
                action.run();
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                contextAtEnd.set(CONTEXT.get());
            }
        }, "context-" + context);
        thread.start();
        thread.join(5_000);
        assertThat(thread.isAlive()).isFalse();
        assertThat(failure.get()).isNull();
        return contextAtEnd.get();
    }

    // --- tasks that never wait in the queue

    @Test
    public void syncInvocationSeesItsOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(completedInvocation, 2, 2, false);

        CONTEXT.set("A");
        assertThat(bulkhead.apply(sync(null)).awaitBlocking()).isEqualTo("A");

        CONTEXT.set("B");
        assertThat(bulkhead.apply(sync(null)).awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", "B");
    }

    @Test
    public void asyncInvocationThatIsNotQueuedSeesItsOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(completedInvocation, 2, 2, false);

        CONTEXT.set("A");
        assertThat(bulkhead.apply(async(null)).awaitBlocking()).isEqualTo("A");

        CONTEXT.set("B");
        assertThat(bulkhead.apply(async(null)).awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", "B");
    }

    // --- queued tasks

    @Test
    public void queuedTaskSeesItsOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(pendingInvocation, 1, 1, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));

        CONTEXT.remove();

        // only the 1st invocation happened so far, the 2nd one is queued
        assertThat(observedContexts).containsExactly("A");

        // completing the 1st invocation on a thread that has no context lets the queued task run
        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext(null, () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");

        nextPendingInvocation().complete("B");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", "B");
    }

    @Test
    public void multipleQueuedTasksEachSeeTheirOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(pendingInvocation, 1, 3, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));
        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));
        CONTEXT.set("C");
        Future<String> resultC = bulkhead.apply(async(null));
        CONTEXT.set("D");
        Future<String> resultD = bulkhead.apply(async(null));
        CONTEXT.remove();

        assertThat(observedContexts).containsExactly("A");

        // each completion happens on a thread with an unrelated context
        // and lets the next queued task run on that very thread
        for (String value : List.of("A", "B", "C", "D")) {
            Completer<String> completer = nextPendingInvocation();
            runOnThreadWithContext("unrelated" + value, () -> completer.complete(value));
        }

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");
        assertThat(resultC.awaitBlocking()).isEqualTo("C");
        assertThat(resultD.awaitBlocking()).isEqualTo("D");

        assertThat(observedContexts).containsExactly("A", "B", "C", "D");
    }

    @Test
    public void queuedTasksCompletingSynchronouslyEachSeeTheirOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(currentInvocation, 1, 3, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        // all the queued invocations complete immediately, so the queue is drained
        // in a loop on the single thread that completes the 1st invocation
        currentInvocationStrategy.set(completedInvocation);

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));
        CONTEXT.set("C");
        Future<String> resultC = bulkhead.apply(async(null));
        CONTEXT.set("D");
        Future<String> resultD = bulkhead.apply(async(null));
        CONTEXT.remove();

        assertThat(observedContexts).containsExactly("A");

        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");
        assertThat(resultC.awaitBlocking()).isEqualTo("C");
        assertThat(resultD.awaitBlocking()).isEqualTo("D");

        assertThat(observedContexts).containsExactly("A", "B", "C", "D");
    }

    @Test
    public void queuedTaskWithoutContextDoesNotInheritContextOfRunningThread() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(pendingInvocation, 1, 1, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        // the queued invocation has no context at all
        CONTEXT.remove();
        Future<String> resultB = bulkhead.apply(async(null));

        // the thread that ends up running the queued task does have a context,
        // but the task must not see it
        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");

        nextPendingInvocation().complete("B");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", null);
    }

    @Test
    public void contextOfRunningThreadIsRestoredAfterQueuedTaskRuns() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(currentInvocation, 1, 1, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        currentInvocationStrategy.set(completedInvocation);

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));

        CONTEXT.remove();

        Completer<String> completerA = nextPendingInvocation();
        String contextAfterCompletion = runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        // the completing thread ran the queued task, but its own context must survive that
        assertThat(contextAfterCompletion).isEqualTo("unrelated");

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");
        assertThat(observedContexts).containsExactly("A", "B");
    }

    @Test
    public void contextIsCapturedWhenTaskIsQueuedNotWhenItRuns() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(pendingInvocation, 1, 1, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));

        // the thread that queued the task moves on to something else before the task runs
        CONTEXT.set("something-else");

        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext(null, () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");

        nextPendingInvocation().complete("B");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", "B");
    }

    // --- error and rejection paths

    @Test
    public void queuedTaskAfterSynchronouslyThrowingTaskSeesItsOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(currentInvocation, 1, 2, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        // the 1st queued invocation throws directly, the 2nd one succeeds
        currentInvocationStrategy.set(ctx -> {
            String context = CONTEXT.get();
            observedContexts.add(context);
            if ("B".equals(context)) {
                throw sneakyThrow(new TestException());
            }
            return Future.of(context);
        });

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));
        CONTEXT.set("C");
        Future<String> resultC = bulkhead.apply(async(null));
        CONTEXT.remove();

        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        assertThatThrownBy(resultB::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // the failure of the 2nd task must not disturb the 3rd task
        assertThat(resultC.awaitBlocking()).isEqualTo("C");

        assertThat(observedContexts).containsExactly("A", "B", "C");
    }

    @Test
    public void queuedTaskAfterTaskFailingWithErrorSeesItsOwnContext() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(currentInvocation, 1, 2, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));

        // the 1st queued invocation returns an already failed future, the 2nd one succeeds
        currentInvocationStrategy.set(ctx -> {
            String context = CONTEXT.get();
            observedContexts.add(context);
            if ("B".equals(context)) {
                return Future.ofError(new TestException());
            }
            return Future.of(context);
        });

        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));
        CONTEXT.set("C");
        Future<String> resultC = bulkhead.apply(async(null));
        CONTEXT.remove();

        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        assertThatThrownBy(resultB::awaitBlocking).isExactlyInstanceOf(TestException.class);
        // the failure of the 2nd task must not disturb the 3rd task
        assertThat(resultC.awaitBlocking()).isEqualTo("C");

        assertThat(observedContexts).containsExactly("A", "B", "C");
    }

    @Test
    public void rejectedTaskDoesNotDisturbContextOfQueuedTask() throws Throwable {
        Bulkhead<String> bulkhead = bulkhead(pendingInvocation, 1, 1, false);

        CONTEXT.set("A");
        Future<String> resultA = bulkhead.apply(async(null));
        CONTEXT.set("B");
        Future<String> resultB = bulkhead.apply(async(null));

        // the bulkhead is full, this one is rejected and never invoked
        CONTEXT.set("C");
        Future<String> resultC = bulkhead.apply(async(null));
        assertThatThrownBy(resultC::awaitBlocking).isInstanceOf(BulkheadException.class);

        CONTEXT.remove();

        Completer<String> completerA = nextPendingInvocation();
        runOnThreadWithContext("unrelated", () -> completerA.complete("A"));

        assertThat(resultA.awaitBlocking()).isEqualTo("A");
        nextPendingInvocation().complete("B");
        assertThat(resultB.awaitBlocking()).isEqualTo("B");

        assertThat(observedContexts).containsExactly("A", "B");
    }

    // --- synchronous queueing (pseudo-asynchronous invocations)

    @Test
    public void syncQueueingTaskSeesContextOfItsOwnThread() throws Throwable {
        // with sync queueing, each invocation waits on its own thread and so keeps
        // the context of that thread; make sure nothing overwrites it
        Party party = Party.create(1);
        Bulkhead<String> bulkhead = bulkhead(ctx -> {
            String context = CONTEXT.get();
            observedContexts.add(context);
            if ("A".equals(context)) {
                // the 1st invocation occupies the bulkhead until the party is disbanded
                try {
                    party.participant().attend();
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            return Future.of(CONTEXT.get());
        }, 1, 1, true);

        AtomicReference<String> resultA = new AtomicReference<>();
        AtomicReference<String> resultB = new AtomicReference<>();

        Thread threadA = invokeOnThreadWithContext(bulkhead, "A", resultA);
        party.organizer().waitForAll(); // the 1st invocation is inside the bulkhead

        Thread threadB = invokeOnThreadWithContext(bulkhead, "B", resultB);
        // the 2nd invocation entered the bulkhead and now sits in the queue
        await().atMost(5, TimeUnit.SECONDS).until(() -> bulkhead.getAvailableCapacityPermits() == 0);

        party.organizer().disband();
        threadA.join(5_000);
        threadB.join(5_000);
        assertThat(threadA.isAlive()).isFalse();
        assertThat(threadB.isAlive()).isFalse();

        assertThat(resultA.get()).isEqualTo("A");
        assertThat(resultB.get()).isEqualTo("B");
        assertThat(observedContexts).containsExactly("A", "B");
    }

    private Thread invokeOnThreadWithContext(Bulkhead<String> bulkhead, String context, AtomicReference<String> result) {
        Thread thread = new Thread(() -> {
            CONTEXT.set(context);
            try {
                result.set(bulkhead.apply(sync(null)).awaitBlocking());
            } catch (Throwable e) {
                throw new AssertionError(e);
            }
        }, "thread-" + context);
        thread.start();
        return thread;
    }
}
