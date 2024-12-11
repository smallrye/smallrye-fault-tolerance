package io.smallrye.faulttolerance.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimeoutExecutionTest {
    private TimeoutExecution execution;
    private AtomicBoolean timedOut;

    @BeforeEach
    public void setUp() {
        execution = new TimeoutExecution(Thread.currentThread(), () -> timedOut.set(true));
        timedOut = new AtomicBoolean(false);
    }

    @Test
    public void initialState() {
        assertThat(execution.isRunning()).isTrue();
    }

    @Test
    public void finish() {
        AtomicBoolean flag = new AtomicBoolean(false);
        execution.finish(() -> flag.set(true));
        assertThat(execution.hasFinished()).isTrue();
        assertThat(flag).isTrue();
    }

    @Test
    public void timeout() {
        execution.timeoutAndInterrupt();
        assertThat(execution.hasTimedOut()).isTrue();
        assertThat(Thread.interrupted()).isTrue(); // clear the current thread interruption status
        assertThat(timedOut).isTrue();
    }

    @Test
    public void timeoutAfterFinish() {
        AtomicBoolean flag = new AtomicBoolean(false);
        execution.finish(() -> flag.set(true));
        execution.timeoutAndInterrupt();
        assertThat(execution.hasFinished()).isTrue();
        assertThat(execution.hasTimedOut()).isFalse();
        assertThat(flag).isTrue();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
        assertThat(timedOut).isFalse();
    }

    @Test
    public void finishAfterTimeout() {
        AtomicBoolean flag = new AtomicBoolean(false);
        execution.timeoutAndInterrupt();
        execution.finish(() -> flag.set(true));
        assertThat(execution.hasFinished()).isFalse();
        assertThat(execution.hasTimedOut()).isTrue();
        assertThat(flag).isFalse();
        assertThat(Thread.interrupted()).isTrue(); // clear the current thread interruption status
        assertThat(timedOut).isTrue();
    }
}
