package io.smallrye.faulttolerance.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.scheduler.Timer;

public class TimerTimeoutWatcherTest {
    private ExecutorService executor;
    private Timer timer;
    private TimerTimeoutWatcher watcher;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        timer = new Timer(executor);
        watcher = new TimerTimeoutWatcher(timer);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
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

    // TODO waiting for a condition in a unit test shouldn't really be needed
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
