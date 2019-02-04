package com.github.ladicek.oaken_ocean.core.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SetOfThrowablesTest {
    @Test
    public void normal_emptySet() {
        SetOfThrowables set = SetOfThrowables.create(Collections.emptyList());

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isFalse();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void normal_singletonSet_throwable() {
        SetOfThrowables set = SetOfThrowables.create(Collections.singletonList(Throwable.class));

        assertThat(set.includes(Throwable.class)).isTrue();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }

    @Test
    public void normal_singletonSet_exception() {
        SetOfThrowables set = SetOfThrowables.create(Collections.singletonList(Exception.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void normal_twoElements_throwableAndException() {
        SetOfThrowables set = SetOfThrowables.create(Arrays.asList(Throwable.class, Exception.class));

        assertThat(set.includes(Throwable.class)).isTrue();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }

    @Test
    public void normal_twoElements_runtimeExceptionAndError() {
        SetOfThrowables set = SetOfThrowables.create(Arrays.asList(RuntimeException.class, Error.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }

    @Test
    public void withoutCustomThrowables_emptySet() {
        SetOfThrowables set = SetOfThrowables.withoutCustomThrowables(Collections.emptyList());

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isFalse();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void withoutCustomThrowables_singletonSet_throwable() {
        SetOfThrowables set = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Throwable.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isFalse();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void withoutCustomThrowables_singletonSet_exception() {
        SetOfThrowables set = SetOfThrowables.withoutCustomThrowables(Collections.singletonList(Exception.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void withoutCustomThrowables_twoElements_throwableAndException() {
        SetOfThrowables set = SetOfThrowables.withoutCustomThrowables(Arrays.asList(Throwable.class, Exception.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void withoutCustomThrowables_twoElements_runtimeExceptionAndError() {
        SetOfThrowables set = SetOfThrowables.withoutCustomThrowables(Arrays.asList(RuntimeException.class, Error.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }
}
