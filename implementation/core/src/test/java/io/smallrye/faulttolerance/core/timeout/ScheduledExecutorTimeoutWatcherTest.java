package io.smallrye.faulttolerance.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class ScheduledExecutorTimeoutWatcherTest {
    private ScheduledExecutorService executor;
    private ScheduledExecutorTimeoutWatcher watcher;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor();
        watcher = new ScheduledExecutorTimeoutWatcher(executor);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void timedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        TimeoutExecution execution = new TimeoutExecution(thread, 100L);
        TimeoutWatch watch = watcher.schedule(execution);

        assertThat(execution.isRunning()).isTrue();
        assertThat(watch.isRunning()).isTrue();

        thread.join();
        assertThat(wasInterrupted).isTrue();

        assertThat(execution.isRunning()).isFalse();
        assertThat(execution.hasFinished()).isFalse();
        assertThat(execution.hasTimedOut()).isTrue();

        assertThatWithin(100, "watch not running", () -> !watch.isRunning());
    }

    @Test
    public void notTimedOut() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Thread thread = run(wasInterrupted);
        TimeoutExecution execution = new TimeoutExecution(thread, 300L);
        TimeoutWatch watch = watcher.schedule(execution);

        assertThat(execution.isRunning()).isTrue();
        assertThat(watch.isRunning()).isTrue();

        thread.join();
        execution.finish(watch::cancel); // execution.finish() needs to be called explicitly
        assertThat(wasInterrupted).isFalse();

        assertThat(execution.isRunning()).isFalse();
        assertThat(execution.hasFinished()).isTrue();
        assertThat(execution.hasTimedOut()).isFalse();
        assertThatWithin(100, "watch not running", () -> !watch.isRunning());
    }

    private static void assertThatWithin(int timeoutMs, String message, Supplier<Boolean> test) {
        await(message).pollInterval(Duration.ofMillis(50))
                .atMost(Duration.ofMillis(timeoutMs))
                .until(test::get);
    }

    private Thread run(AtomicBoolean interruptionFlag) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                interruptionFlag.set(true);
            }
        });
        thread.start();
        return thread;
    }
}
