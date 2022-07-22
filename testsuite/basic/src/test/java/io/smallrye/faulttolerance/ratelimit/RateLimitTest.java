package io.smallrye.faulttolerance.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RateLimitTest {
    private static final int TASKS_COUNT = 2 * MyService.RATE_LIMIT;

    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(TASKS_COUNT);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void fixedWindowNoSpacing(MyService service) throws ExecutionException, InterruptedException {
        List<Callable<String>> tasks = new ArrayList<>(TASKS_COUNT);
        for (int i = 0; i < TASKS_COUNT; i++) {
            tasks.add(service::fixedWindowNoSpacing);
        }
        List<Future<String>> futures = executorService.invokeAll(tasks);

        List<String> results = new ArrayList<>(TASKS_COUNT);
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(TASKS_COUNT);

        assertThat(results).filteredOn("hello"::equals).hasSize(MyService.RATE_LIMIT);
        assertThat(results).filteredOn("fallback"::equals).hasSize(MyService.RATE_LIMIT);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void fixedWindowWithSpacing(MyService service) throws ExecutionException, InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        List<Callable<String>> tasks = new ArrayList<>(TASKS_COUNT);
        for (int i = 0; i < TASKS_COUNT; i++) {
            tasks.add(() -> {
                int id = counter.getAndIncrement();
                if (id < 10) {
                    Thread.sleep(id * 100L);
                } else {
                    Thread.sleep(2000);
                }
                return service.fixedWindowWithSpacing();
            });
        }
        List<Future<String>> futures = executorService.invokeAll(tasks);

        List<String> results = new ArrayList<>(TASKS_COUNT);
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(TASKS_COUNT);

        assertThat(results).filteredOn("hello"::equals)
                .hasSizeGreaterThanOrEqualTo(10)
                .hasSizeLessThanOrEqualTo(MyService.RATE_LIMIT);
        assertThat(results).filteredOn("fallback"::equals).hasSizeGreaterThanOrEqualTo(MyService.RATE_LIMIT);
    }

    @Test
    public void rollingWindowNoSpacing(MyService service) throws ExecutionException, InterruptedException {
        List<Callable<String>> tasks = new ArrayList<>(TASKS_COUNT);
        for (int i = 0; i < TASKS_COUNT; i++) {
            tasks.add(service::rollingWindowNoSpacing);
        }
        List<Future<String>> futures = executorService.invokeAll(tasks);

        List<String> results = new ArrayList<>(TASKS_COUNT);
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(TASKS_COUNT);

        assertThat(results).filteredOn("hello"::equals).hasSize(MyService.RATE_LIMIT);
        assertThat(results).filteredOn("fallback"::equals).hasSize(MyService.RATE_LIMIT);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void rollingWindowWithSpacing(MyService service) throws ExecutionException, InterruptedException {
        AtomicInteger counter = new AtomicInteger();
        List<Callable<String>> tasks = new ArrayList<>(TASKS_COUNT);
        for (int i = 0; i < TASKS_COUNT; i++) {
            tasks.add(() -> {
                int id = counter.getAndIncrement();
                if (id < 10) {
                    Thread.sleep(id * 100L);
                } else {
                    Thread.sleep(2000);
                }
                return service.rollingWindowWithSpacing();
            });
        }
        List<Future<String>> futures = executorService.invokeAll(tasks);

        List<String> results = new ArrayList<>(TASKS_COUNT);
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertThat(results).hasSize(TASKS_COUNT);

        assertThat(results).filteredOn("hello"::equals)
                .hasSizeGreaterThanOrEqualTo(10)
                .hasSizeLessThanOrEqualTo(MyService.RATE_LIMIT);
        assertThat(results).filteredOn("fallback"::equals).hasSizeGreaterThanOrEqualTo(MyService.RATE_LIMIT);
    }
}
