package com.github.ladicek.oaken_ocean.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScheduledExecutorTimeoutWatcherTest {
    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void timedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        TimeoutExecution execution = new TimeoutExecution(thread, 50L);
        TimeoutWatch watch = watcher.schedule(execution);

        assertThat(execution.isRunning()).isTrue();
        assertThat(watch.isRunning()).isTrue();

        thread.join();
        assertThat(wasInterrupted).isTrue();

        assertThat(execution.isRunning()).isFalse();
        assertThat(execution.hasFinished()).isFalse();
        assertThat(execution.hasTimedOut()).isTrue();
        assertThat(watch.isRunning()).isFalse();
    }

    @Test
    public void notTimedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        TimeoutExecution execution = new TimeoutExecution(thread, 200L);
        TimeoutWatch watch = watcher.schedule(execution);

        assertThat(execution.isRunning()).isTrue();
        assertThat(watch.isRunning()).isTrue();

        thread.join();
        execution.finish(watch::cancel); // execution.finish() needs to be called explicitly
        assertThat(wasInterrupted).isFalse();

        assertThat(execution.isRunning()).isFalse();
        assertThat(execution.hasFinished()).isTrue();
        assertThat(execution.hasTimedOut()).isFalse();
        assertThat(watch.isRunning()).isFalse();
    }

    private Thread run(AtomicBoolean interruptionFlag) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                interruptionFlag.set(true);
            }
        });
        thread.start();
        return thread;
    }
}
