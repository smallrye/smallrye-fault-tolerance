package io.smallrye.faulttolerance.core.timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.core.util.party.Party;

@EnabledOnOs(OS.LINUX)
public class ThreadTimerStressTest {
    private static final int ITERATIONS = 100;
    private static final int TASKS_PER_ITERATION = 100;
    private static final long DELAY_INCREMENT = 50;

    // shouldn't be too big, otherwise context switching cost will start to dominate
    private static final int POOL_SIZE = TASKS_PER_ITERATION + 10;

    private ExecutorService executor;
    private Timer timer;

    @BeforeEach
    public void setUp() throws InterruptedException {
        executor = Executors.newFixedThreadPool(POOL_SIZE);
        timer = new ThreadTimer(executor);

        // precreate all threads in the pool
        // if we didn't do this, the first few iterations would be dominated
        // by the cost of creating threads
        Party party = Party.create(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            executor.submit(() -> {
                try {
                    party.participant().attend();
                } catch (InterruptedException ignored) {
                }
            });
        }
        party.organizer().waitForAll();
        party.organizer().disband();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        timer.shutdown();
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void stressTest() throws InterruptedException {
        // this test assumes that ConcurrentHashMap scales better than the Timer
        ConcurrentMap<String, Long> deltas = new ConcurrentHashMap<>();

        long delay = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            delay += DELAY_INCREMENT;

            for (int j = 0; j < TASKS_PER_ITERATION; j++) {
                String taskId = i + "_" + j;

                long desiredTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delay);
                timer.schedule(delay, () -> {
                    long now = System.nanoTime();
                    long delta = TimeUnit.NANOSECONDS.toMillis(now - desiredTime);
                    deltas.put(taskId, delta);
                });
            }
        }

        Thread.sleep(delay + DELAY_INCREMENT);

        assertThat(deltas).hasSize(ITERATIONS * TASKS_PER_ITERATION);

        for (Map.Entry<String, Long> entry : deltas.entrySet()) {
            String id = entry.getKey();
            Long delta = entry.getValue();

            assertThat(delta)
                    .as("task " + id + " has delta " + delta)
                    .isCloseTo(0L, byLessThan(DELAY_INCREMENT));
        }
    }
}
