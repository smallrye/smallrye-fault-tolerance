package com.github.ladicek.oaken_ocean.core.timeout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduledExecutorTimeoutWatcherTest {
    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void timedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        watcher.schedule(new TimeoutExecution(thread, 50L));
        thread.join();

        assertThat(wasInterrupted).isTrue();
    }

    @Test
    public void notTimedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        watcher.schedule(new TimeoutExecution(thread, 200L));
        thread.join();

        assertThat(wasInterrupted).isFalse();
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
