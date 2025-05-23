package io.smallrye.faulttolerance.core.timeout;

import static io.smallrye.faulttolerance.core.util.TestThread.runOnTestThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.faulttolerance.core.util.TestInvocation;
import io.smallrye.faulttolerance.core.util.TestThread;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class TimeoutSyncTest {
    private Barrier timerElapsedBarrier;
    private Barrier timerTaskFinishedBarrier;

    private TestTimer timer;

    @BeforeEach
    public void setUp() {
        timerElapsedBarrier = Barrier.interruptible();
        timerTaskFinishedBarrier = Barrier.interruptible();

        timer = new TestTimer(timerElapsedBarrier, timerTaskFinishedBarrier);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
    }

    @Test
    public void negativeTimeout() {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        assertThatThrownBy(() -> new Timeout<>(invocation, "test invocation", -1, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void zeroTimeout() {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        assertThatThrownBy(() -> new Timeout<>(invocation, "test invocation", 0, timer))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void immediatelyReturning_value() throws Exception {
        TestInvocation<String> invocation = TestInvocation.of(() -> "foobar");
        Timeout<String> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<String> result = runOnTestThread(timeout, false);
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void immediatelyReturning_exception() {
        TestInvocation<Void> invocation = TestInvocation.of(TestException::doThrow);
        Timeout<Void> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<Void> result = runOnTestThread(timeout, false);
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_notTimedOut() throws Exception {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        Timeout<String> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<String> result = runOnTestThread(timeout, false);
        delayBarrier.open();
        assertThat(result.await()).isEqualTo("foobar");
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_value_timedOut() throws InterruptedException {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        Timeout<String> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<String> result = runOnTestThread(timeout, false);
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_timedOutNoninterruptibly() throws InterruptedException {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            return "foobar";
        });
        Timeout<String> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<String> result = runOnTestThread(timeout, false);
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_value_interruptedEarly() throws InterruptedException {
        Party party = Party.create(1);

        TestInvocation<String> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            return "foobar";
        });
        Timeout<String> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<String> executingThread = runOnTestThread(timeout, false);
        party.organizer().waitForAll();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_notTimedOut() {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        Timeout<Void> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<Void> result = runOnTestThread(timeout, false);
        delayBarrier.open();
        assertThatThrownBy(result::await).isExactlyInstanceOf(TestException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }

    @Test
    public void delayed_exception_timedOut() throws InterruptedException {
        Barrier delayBarrier = Barrier.interruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        Timeout<Void> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<Void> result = runOnTestThread(timeout, false);
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_timedOutNoninterruptibly() throws InterruptedException {
        Barrier delayBarrier = Barrier.noninterruptible();

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            delayBarrier.await();
            throw new TestException();
        });
        Timeout<Void> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<Void> result = runOnTestThread(timeout, false);
        timerElapsedBarrier.open();
        timerTaskFinishedBarrier.await();
        delayBarrier.open();
        assertThatThrownBy(result::await)
                .isExactlyInstanceOf(TimeoutException.class)
                .hasMessage("test invocation timed out");
        assertThat(timer.timerTaskCancelled()).isFalse();
    }

    @Test
    public void delayed_exception_interruptedEarly() throws InterruptedException {
        Party party = Party.create(1);

        TestInvocation<Void> invocation = TestInvocation.of(() -> {
            party.participant().attend();
            throw new TestException();
        });
        Timeout<Void> timeout = new Timeout<>(invocation, "test invocation", 1000, timer);
        TestThread<Void> executingThread = runOnTestThread(timeout, false);
        party.organizer().waitForAll();
        executingThread.interrupt();
        assertThatThrownBy(executingThread::await).isExactlyInstanceOf(InterruptedException.class);
        assertThat(timer.timerTaskCancelled()).isTrue();
    }
}
