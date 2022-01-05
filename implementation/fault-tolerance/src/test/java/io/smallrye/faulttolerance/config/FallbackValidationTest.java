package io.smallrye.faulttolerance.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class FallbackValidationTest {
    @Test
    public void boxing() {
        assertNull(FallbackValidation.box(null));

        assertEquals(Boolean.class, FallbackValidation.box(boolean.class));
        assertEquals(Byte.class, FallbackValidation.box(byte.class));
        assertEquals(Short.class, FallbackValidation.box(short.class));
        assertEquals(Integer.class, FallbackValidation.box(int.class));
        assertEquals(Long.class, FallbackValidation.box(long.class));
        assertEquals(Float.class, FallbackValidation.box(float.class));
        assertEquals(Double.class, FallbackValidation.box(double.class));
        assertEquals(Character.class, FallbackValidation.box(char.class));

        assertEquals(Boolean.class, FallbackValidation.box(Boolean.class));
        assertEquals(Byte.class, FallbackValidation.box(Byte.class));
        assertEquals(Short.class, FallbackValidation.box(Short.class));
        assertEquals(Integer.class, FallbackValidation.box(Integer.class));
        assertEquals(Long.class, FallbackValidation.box(Long.class));
        assertEquals(Float.class, FallbackValidation.box(Float.class));
        assertEquals(Double.class, FallbackValidation.box(Double.class));
        assertEquals(Character.class, FallbackValidation.box(Character.class));

        assertEquals(Object.class, FallbackValidation.box(Object.class));
        assertEquals(Number.class, FallbackValidation.box(Number.class));
        assertEquals(String.class, FallbackValidation.box(String.class));
    }
}
