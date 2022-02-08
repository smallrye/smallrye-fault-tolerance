package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

public class CallbacksTest {
    @Test
    public void nullConsumer() {
        assertThat(Callbacks.wrap((Consumer<Object>) null)).isNull();
    }

    @Test
    public void consumer() {
        AtomicReference<String> value = new AtomicReference<>();

        Callbacks.wrap(value::set).accept("hello");

        assertThat(value).hasValue("hello");
    }

    @Test
    public void throwingConsumer() {
        AtomicReference<String> value = new AtomicReference<>();

        assertThatCode(() -> {
            Callbacks.wrap((String string) -> {
                value.set(string);
                throw new RuntimeException();
            }).accept("hello");
        }).doesNotThrowAnyException();

        assertThat(value).hasValue("hello");
    }

    @Test
    public void nullRunnable() {
        assertThat(Callbacks.wrap((Runnable) null)).isNull();
    }

    @Test
    public void runnable() {
        AtomicReference<String> value = new AtomicReference<>();

        Callbacks.wrap(() -> {
            value.set("hello");
        }).run();

        assertThat(value).hasValue("hello");
    }

    @Test
    public void throwingRunnable() {
        AtomicReference<String> value = new AtomicReference<>();

        assertThatCode(() -> {
            Callbacks.wrap(() -> {
                value.set("hello");
                throw new RuntimeException();
            }).run();
        }).doesNotThrowAnyException();

        assertThat(value).hasValue("hello");
    }
}
