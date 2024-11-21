package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.Action.startThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;

public class FutureTest {
    @Test
    public void singleThread_completeBeforeCallback_success() {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.complete("foobar");

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
    }

    @Test
    public void singleThread_completeBeforeCallback_failure() {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.completeWithError(new TestException());

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
    }

    @Test
    public void singleThread_completeAfterCallback_success() {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        completer.complete("foobar");

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
    }

    @Test
    public void singleThread_completeAfterCallback_failure() {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        completer.completeWithError(new TestException());

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
    }

    @Test
    public void singleThread_cancelBeforeCompletion_success() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        completer.future().cancel();

        completer.complete("foobar");

        assertThat(result).hasValue(null);
        assertThat(failure).hasValue(null);
        assertThat(cancelled).isTrue();
    }

    @Test
    public void singleThread_cancelBeforeCompletion_failure() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        completer.future().cancel();

        completer.completeWithError(new TestException());

        assertThat(result).hasValue(null);
        assertThat(failure).hasValue(null);
        assertThat(cancelled).isTrue();
    }

    @Test
    public void singleThread_cancelAfterCompletion_success() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.complete("foobar");

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        completer.future().cancel();

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
        assertThat(cancelled).isFalse();
    }

    @Test
    public void singleThread_cancelAfterCompletion_failure() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer.completeWithError(new TestException());

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        completer.future().cancel();

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
        assertThat(cancelled).isFalse();
    }

    @Test
    public void singleThread_multipleCallbacks() {
        Completer<String> completer = Completer.create();

        assertThatCode(() -> {
            completer.future().then((value, error) -> {
            });
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            completer.future().then((value, error) -> {
            });
        }).isInstanceOf(IllegalStateException.class);
    }

    // ---

    @Test
    public void twoThreads_completeBeforeCallback_success() throws InterruptedException {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completer.complete("foobar");
            deliveryBarrier.open();
        });

        deliveryBarrier.await();
        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
        assertThat(deliveryThread).hasValue(Thread.currentThread());
    }

    @Test
    public void twoThreads_completeBeforeCallback_failure() throws InterruptedException {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completer.completeWithError(new TestException());
            deliveryBarrier.open();
        });

        deliveryBarrier.await();
        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
        assertThat(deliveryThread).hasValue(Thread.currentThread());
    }

    @Test
    public void twoThreads_completeAfterCallback_success() throws InterruptedException {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier completionBarrier = Barrier.noninterruptible();
        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completionBarrier.await();
            completer.complete("foobar");
            deliveryBarrier.open();
        });

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completionBarrier.open();
        deliveryBarrier.await();

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
        assertThat(deliveryThread).hasValueMatching(thread -> thread != Thread.currentThread());
    }

    @Test
    public void twoThreads_completeAfterCallback_failure() throws InterruptedException {
        Completer<String> completer = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier completionBarrier = Barrier.noninterruptible();
        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completionBarrier.await();
            completer.completeWithError(new TestException());
            deliveryBarrier.open();
        });

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completionBarrier.open();
        deliveryBarrier.await();

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
        assertThat(deliveryThread).hasValueMatching(thread -> thread != Thread.currentThread());
    }

    @Test
    public void twoThreads_cancelBeforeCompletion_success() throws InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier completionBarrier = Barrier.noninterruptible();
        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completionBarrier.await();
            completer.complete("foobar");
            deliveryBarrier.open();
        });

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completer.future().cancel();
        completionBarrier.open();
        deliveryBarrier.await();

        assertThat(result).hasValue(null);
        assertThat(failure).hasValue(null);
        assertThat(deliveryThread).hasValue(null);
        assertThat(cancelled).isTrue();
    }

    @Test
    public void twoThreads_cancelBeforeCompletion_failure() throws InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier completionBarrier = Barrier.noninterruptible();
        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completionBarrier.await();
            completer.completeWithError(new TestException());
            deliveryBarrier.open();
        });

        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completer.future().cancel();
        completionBarrier.open();
        deliveryBarrier.await();

        assertThat(result).hasValue(null);
        assertThat(failure).hasValue(null);
        assertThat(deliveryThread).hasValue(null);
        assertThat(cancelled).isTrue();
    }

    @Test
    public void twoThreads_cancelAfterCompletion_success() throws InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completer.complete("foobar");
            deliveryBarrier.open();
        });

        deliveryBarrier.await();
        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completer.future().cancel();

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
        assertThat(deliveryThread).hasValue(Thread.currentThread());
        assertThat(cancelled).isFalse();
    }

    @Test
    public void twoThreads_cancelAfterCompletion_failure() throws InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        Completer<String> completer = Completer.create();
        completer.onCancel(() -> cancelled.set(true));
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> deliveryThread = new AtomicReference<>();

        Barrier deliveryBarrier = Barrier.noninterruptible();
        startThread(() -> {
            completer.completeWithError(new TestException());
            deliveryBarrier.open();
        });

        deliveryBarrier.await();
        completer.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
            deliveryThread.set(Thread.currentThread());
        });
        completer.future().cancel();

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
        assertThat(deliveryThread).hasValue(Thread.currentThread());
        assertThat(cancelled).isFalse();
    }

    @Test
    public void twoThreads_multipleCallbacks() throws InterruptedException {
        Completer<String> completer = Completer.create();

        Barrier barrier = Barrier.noninterruptible();
        startThread(() -> {
            completer.future().then((value, error) -> {
            });
            barrier.open();
        });

        barrier.await();
        assertThatCode(() -> {
            completer.future().then((value, error) -> {
            });
        }).isInstanceOf(IllegalStateException.class);
    }

    // ---
    // tests for the convenience helpers

    @Test
    public void futureThenComplete_success() {
        Completer<String> completer1 = Completer.create();
        Completer<String> completer2 = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer1.future().thenComplete(completer2);

        completer2.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        completer1.complete("foobar");

        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
    }

    @Test
    public void futureThenComplete_failure() {
        Completer<String> completer1 = Completer.create();
        Completer<String> completer2 = Completer.create();
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        completer1.future().thenComplete(completer2);

        completer2.future().then((value, error) -> {
            result.set(value);
            failure.set(error);
        });

        completer1.completeWithError(new TestException());

        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
    }

    @Test
    public void futureOfValue() {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Future<String> future = Future.of("foobar");
        future.then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
    }

    @Test
    public void futureOfError() {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Future<String> future = Future.ofError(new TestException());
        future.then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
    }

    @Test
    public void futureFromValue() {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Future<String> future = Future.from(() -> "foobar");
        future.then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        assertThat(result).hasValue("foobar");
        assertThat(failure).hasValue(null);
    }

    @Test
    public void futureFromError() {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Future<String> future = Future.from(() -> {
            throw new TestException();
        });
        future.then((value, error) -> {
            result.set(value);
            failure.set(error);
        });
        assertThat(result).hasValue(null);
        assertThat(failure).hasValueMatching(error -> error instanceof TestException);
    }

    // ---
    // tests for the blocking part of the API

    @Test
    public void isComplete_success() {
        Completer<String> completer = Completer.create();

        assertThat(completer.future().isComplete()).isFalse();

        completer.complete("foobar");

        assertThat(completer.future().isComplete()).isTrue();
    }

    @Test
    public void isComplete_failure() {
        Completer<String> completer = Completer.create();

        assertThat(completer.future().isComplete()).isFalse();

        completer.completeWithError(new TestException());

        assertThat(completer.future().isComplete()).isTrue();
    }

    @Test
    public void awaitBlocking_completeBeforeBlocking_success() throws Throwable {
        Completer<String> completer = Completer.create();
        completer.complete("foobar");

        assertThat(completer.future().awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void awaitBlocking_completeBeforeBlocking_failure() {
        Completer<String> completer = Completer.create();
        completer.completeWithError(new TestException());

        assertThatCode(completer.future()::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }

    @Test
    public void awaitBlocking_completeAfterBlocking_success() throws Throwable {
        Completer<String> completer = Completer.create();

        startThread(() -> {
            Thread.sleep(500);
            completer.complete("foobar");
        });

        assertThat(completer.future().awaitBlocking()).isEqualTo("foobar");
    }

    @Test
    public void awaitBlocking_completeAfterBlocking_failure() {
        Completer<String> completer = Completer.create();

        startThread(() -> {
            Thread.sleep(500);
            completer.completeWithError(new TestException());
        });

        assertThatCode(completer.future()::awaitBlocking).isExactlyInstanceOf(TestException.class);
    }
}