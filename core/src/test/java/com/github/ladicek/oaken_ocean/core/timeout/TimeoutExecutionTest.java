package com.github.ladicek.oaken_ocean.core.timeout;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeoutExecutionTest {
    private TimeoutExecution execution;

    @Before
    public void setUp() {
        execution = new TimeoutExecution(Thread.currentThread(), 1000L);
    }

    @Test
    public void initialState() {
        assertThat(execution.isRunning()).isTrue();
    }

    @Test
    public void timeoutValue() {
        assertThat(execution.timeoutInMillis()).isEqualTo(1000L);
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
    }
}
