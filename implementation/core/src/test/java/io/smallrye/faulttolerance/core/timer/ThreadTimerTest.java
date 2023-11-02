package io.smallrye.faulttolerance.core.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

    @Test void simple() throws InterruptedException{
        var tt = new ThreadTimer(Runnable::run);
        var q = new LinkedBlockingQueue<Integer>();
        {
            TimerTask task1 = tt.schedule(50, ()->q.add(1));
            assertThat(task1.isDone()).isFalse();
            assertThat(task1.cancel()).isTrue();
            assertThat(task1.cancel()).isFalse();// already canceled
            assertThat(task1.isDone()).isTrue();
            assertThat(q).hasSize(0);
            Thread.sleep(60);
            assertThat(q).hasSize(0);
        }
        {
            TimerTask task2 = tt.schedule(0, ()->q.add(2));
            Thread.sleep(20);
            assertThat(task2.isDone()).isTrue();
            assertThat(task2.cancel()).isFalse();// already done
            assertThat(task2.isDone()).isTrue();
            assertThat(q).hasSize(1).containsExactly(2);
        }
    }
}
