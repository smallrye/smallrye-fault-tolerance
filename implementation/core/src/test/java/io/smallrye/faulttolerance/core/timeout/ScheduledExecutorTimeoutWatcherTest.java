package io.smallrye.faulttolerance.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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

    // TODO waiting for a condition in this test shouldn't really be needed
    //  ultimately, we should use Awaitility for waiting for a condition in a test, not home-grown utils like this
    private static void assertThatWithin(int timeoutMs, String message, Supplier<Boolean> test) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted waiting for " + message);
            }
            if (test.get()) {
                return;
            }
        }
        fail(message + " not satisfied in " + timeoutMs + "ms");
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
