package io.smallrye.faulttolerance.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PrimitivesTest {
    @Test
    public void clamp() {
        assertThat(Primitives.clamp(1, 2, 3)).isEqualTo(2);
        assertThat(Primitives.clamp(2, 1, 3)).isEqualTo(2);
        assertThat(Primitives.clamp(3, 1, 2)).isEqualTo(2);

        assertThat(Primitives.clamp(1, 1, 3)).isEqualTo(1);
        assertThat(Primitives.clamp(3, 1, 3)).isEqualTo(3);
    }
}
