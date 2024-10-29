package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.FaultToleranceContextUtil.async;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.async.ThreadOffload;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestExecutor;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class TimeoutAsyncTest {
    private Barrier timerElapsedBarrier;
    private Barrier timerTaskFinishedBarrier;

    private TestTimer timer;

    private TestExecutor executor;

    @BeforeEach
    public void setUp() {
        timerElapsedBarrier = Barrier.interruptible();
        timerTaskFinishedBarrier = Barrier.interruptible();

        timer = new TestTimer(timerElapsedBarrier, timerTaskFinishedBarrier);

        executor = new TestExecutor();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        timer.shutdown();
    }

    @Test
    public void negativeTimeout() {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        assertThatThrownBy(() -> new Timeout<>(execution, "test invocation", -1, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        assertThatThrownBy(() -> new Timeout<>(execution, "test invocation", 0, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Throwable {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Timeout<String> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<String> result = timeout.apply(async(null));
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Timeout<Void> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<Void> result = timeout.apply(async(null));
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Throwable {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Timeout<String> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<String> result = timeout.apply(async(null));
        delayBarrier.open();
        assertThat(result.awaitBlocking()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Timeout<String> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<String> result = timeout.apply(async(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::awaitBlocking)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Timeout<String> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<String> result = timeout.apply(async(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::awaitBlocking)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws Exception {
        Party party = Party.create(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "foobar";
        });
        ThreadOffload<String> execution = new ThreadOffload<>(invocation, executor);
        Timeout<String> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<String> result = timeout.apply(async(null));
        party.organizer().waitForAll();
        executor.interruptExecutingThread();
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Timeout<Void> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<Void> result = timeout.apply(async(null));
        delayBarrier.open();
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Timeout<Void> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<Void> result = timeout.apply(async(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::awaitBlocking)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws Exception {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Timeout<Void> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<Void> result = timeout.apply(async(null));
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::awaitBlocking)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessageContaining("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws Exception {
        Party party = Party.create(1);

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            throw new TestException();
        });
        ThreadOffload<Void> execution = new ThreadOffload<>(invocation, executor);
        Timeout<Void> timeout = new Timeout<>(execution, "test invocation", 1000, timer);
        Future<Void> result = timeout.apply(async(null));
        party.organizer().waitForAll();
        executor.interruptExecutingThread();
        assertThatThrownBy(result::awaitBlocking).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }
}
