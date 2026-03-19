package io.smallrye.faulttolerance.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FutureLoopStressTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void allAttemptsSucceed() {
        int max = 50;

        for (int i = 0; i < 10_000; i++) {
            AtomicInteger invocations = new AtomicInteger();

            Future<Integer> result = Future.loop(0, it -> it < max, it -> {
                invocations.incrementAndGet();
                Completer<Integer> completer = Completer.create();
                executor.submit(() -> completer.complete(it + 1));
                return completer.future();
            });

            assertThatCode(result::awaitBlocking).doesNotThrowAnyException();
            assertThat(invocations).hasValue(max);
        }
    }

    @Test
    public void lastAttemptFails() {
        int max = 50;

        for (int i = 0; i < 10_000; i++) {
            AtomicInteger invocations = new AtomicInteger();

            Future<Integer> result = Future.loop(0, it -> it < max, it -> {
                invocations.incrementAndGet();
                if (it < max - 1) {
                    Completer<Integer> completer = Completer.create();
                    executor.submit(() -> completer.complete(it + 1));
                    return completer.future();
                } else {
                    return Future.ofError(new RuntimeException("test failure"));
                }
            });

            assertThatCode(result::awaitBlocking)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test failure");
            assertThat(invocations).hasValue(max);
        }
    }
}
