package io.smallrye.faulttolerance.core.util;

public final class Preconditions {
    private Preconditions() {
        // avoid instantiation
    }

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static int check(int value, boolean test, String message) {
        if (!test) {
            throw new IllegalArgumentException(message + ", was " + value);
        }
        return value;
    }

    public static long check(long value, boolean test, String message) {
        if (!test) {
            throw new IllegalArgumentException(message + ", was " + value);
        }
        return value;
    }

    public static double check(double value, boolean test, String message) {
        if (!test) {
            throw new IllegalArgumentException(message + ", was " + value);
        }
        return value;
    }
}
