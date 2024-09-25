package io.smallrye.faulttolerance.core.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class ThreadTimerTest {
    private ExecutorService executor;
    private Timer timer;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        timer = new ThreadTimer(executor);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void basicUsage() throws InterruptedException {
        Queue<String> queue = new ConcurrentLinkedQueue<>();

        TimerTask fooTask = timer.schedule(600, () -> {
            queue.add("foo");
        });

        TimerTask barTask = timer.schedule(100, () -> {
            queue.add("bar");
        });

        TimerTask bazTask = timer.schedule(400, () -> {
            queue.add("baz");
        });

        // 0 ms since start

        assertThat(queue).isEmpty();
        assertThat(fooTask.isDone()).isFalse();
        assertThat(barTask.isDone()).isFalse();
        assertThat(bazTask.isDone()).isFalse();

        Thread.sleep(200);
        // 200 ms since start

        assertThat(queue).containsExactly("bar");
        assertThat(fooTask.isDone()).isFalse();
        assertThat(barTask.isDone()).isTrue();
        assertThat(bazTask.isDone()).isFalse();

        TimerTask quuxTask = timer.schedule(100, () -> {
            queue.add("quux");
        });

        boolean cancelled = bazTask.cancel();
        assertThat(cancelled).isTrue();
        assertThat(bazTask.isDone()).isTrue();

        Thread.sleep(200);
        // 400 ms since start

        assertThat(queue).containsExactly("bar", "quux");
        assertThat(fooTask.isDone()).isFalse();
        assertThat(barTask.isDone()).isTrue();
        assertThat(quuxTask.isDone()).isTrue();

        Thread.sleep(300);
        // 700 ms since start

        assertThat(queue).containsExactly("bar", "quux", "foo");

        assertThat(fooTask.isDone()).isTrue();
        assertThat(barTask.isDone()).isTrue();
        assertThat(quuxTask.isDone()).isTrue();
    }
}
