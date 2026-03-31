package io.smallrye.faulttolerance.core.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the timer comparator handles {@code identityHashCode} collisions.
 * <p>
 * This test is expected to run in a separate Surefire execution with {@code -XX:hashCode=2},
 * which makes {@code System.identityHashCode()} return 1 for all objects. Without the
 * sequence number tiebreaker in the comparator, tasks with the same start time would
 * violate the comparator contract and could be silently dropped by the skip list.
 * <p>
 * The test uses {@link ThreadTimer#scheduleAtTime(long, Runnable)} to force all tasks
 * to have the exact same start time, guaranteeing a collision.
 */
public class ThreadTimerHashCodeCollisionTest {
    private ExecutorService executor;
    private ThreadTimer timer;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(4);
        timer = new ThreadTimer(executor);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void allTasksFireDespiteHashCodeCollision() throws InterruptedException {
        int taskCount = 100;
        AtomicInteger fired = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(taskCount);

        long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
        for (int i = 0; i < taskCount; i++) {
            timer.scheduleAtTime(startTime, () -> {
                fired.incrementAndGet();
                done.countDown();
            });
        }

        assertThat(done.await(5, TimeUnit.SECONDS))
                .as("all %d tasks should fire", taskCount)
                .isTrue();
        assertThat(fired.get()).isEqualTo(taskCount);
    }
}
