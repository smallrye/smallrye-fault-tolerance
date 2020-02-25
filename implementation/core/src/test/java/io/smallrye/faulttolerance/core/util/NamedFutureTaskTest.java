package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class NamedFutureTaskTest {
    @Test
    public void nameIsSet() {
        String oldThreadName = Thread.currentThread().getName();
        AtomicReference<String> threadNameInsideTask = new AtomicReference<>();

        NamedFutureTask<String> task = new NamedFutureTask<>("TestTask", () -> {
            threadNameInsideTask.set(Thread.currentThread().getName());
            return "foobar";
        });

        task.run();

        assertThat(Thread.currentThread().getName()).isEqualTo(oldThreadName);
        assertThat(threadNameInsideTask.get()).contains("TestTask");
    }
}
