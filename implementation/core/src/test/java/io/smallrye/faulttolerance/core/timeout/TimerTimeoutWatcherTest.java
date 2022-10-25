package io.smallrye.faulttolerance.core.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;

@EnabledOnOs(OS.LINUX)
public class TimerTimeoutWatcherTest {
    private ExecutorService executor;
    private Timer timer;
    private TimerTimeoutWatcher watcher;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        timer = new ThreadTimer(executor);
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

        await("watch not running")
                .atMost(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> !watch.isRunning());
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
        await("watch not running")
                .atMost(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> !watch.isRunning());
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
