package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SetOfThrowablesTest {
    @Test
    public void emptySet() {
        SetOfThrowables set = SetOfThrowables.create(Collections.emptyList());

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isFalse();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void singletonSet_throwable() {
        SetOfThrowables set = SetOfThrowables.create(Collections.singletonList(Throwable.class));

        assertThat(set.includes(Throwable.class)).isTrue();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }

    @Test
    public void singletonSet_exception() {
        SetOfThrowables set = SetOfThrowables.create(Collections.singletonList(Exception.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isFalse();
    }

    @Test
    public void twoElements_throwableAndException() {
        SetOfThrowables set = SetOfThrowables.create(Arrays.asList(Throwable.class, Exception.class));

        assertThat(set.includes(Throwable.class)).isTrue();
        assertThat(set.includes(Exception.class)).isTrue();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }

    @Test
    public void twoElements_runtimeExceptionAndError() {
        SetOfThrowables set = SetOfThrowables.create(Arrays.asList(RuntimeException.class, Error.class));

        assertThat(set.includes(Throwable.class)).isFalse();
        assertThat(set.includes(Exception.class)).isFalse();
        assertThat(set.includes(RuntimeException.class)).isTrue();
        assertThat(set.includes(Error.class)).isTrue();
    }
}
