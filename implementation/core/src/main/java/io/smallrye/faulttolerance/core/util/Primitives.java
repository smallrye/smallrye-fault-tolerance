package io.smallrye.faulttolerance.core.util;

public class Primitives {
    private Primitives() {
        // avoid instantiation
    }

    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }
}
