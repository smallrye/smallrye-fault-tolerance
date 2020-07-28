package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.smallrye.faulttolerance.core.timer.Timer;

public class TimerTest {
    private ExecutorService executor;
    private Timer timer;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        timer = new Timer(executor);
    }

    @After
    public void tearDown() throws InterruptedException {
        timer.shutdown();
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void basicUsage() throws InterruptedException {
        Queue<String> queue = new ConcurrentLinkedQueue<>();

        timer.schedule(500, () -> {
            queue.add("foo");
        });

        timer.schedule(100, () -> {
            queue.add("bar");
        });

        // 0 ms since start

        assertThat(queue).isEmpty();

        Thread.sleep(200);
        // 200 ms since start

        assertThat(queue).containsExactly("bar");

        timer.schedule(100, () -> {
            queue.add("quux");
        });

        Thread.sleep(200);
        // 400 ms since start

        assertThat(queue).containsExactly("bar", "quux");

        Thread.sleep(200);
        // 600 ms since start

        assertThat(queue).containsExactly("bar", "quux", "foo");
    }
}
