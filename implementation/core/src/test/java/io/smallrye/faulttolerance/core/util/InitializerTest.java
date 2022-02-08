package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class InitializerTest {
    @Test
    public void runOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        Initializer initializer = new Initializer(counter::incrementAndGet);

        for (int i = 0; i < 1000; i++) {
            initializer.runOnce();
        }

        assertThat(counter).hasValue(1);
    }
}
