package com.github.ladicek.oaken_ocean.core.util;

public class Preconditions {
    private Preconditions() {
    } // avoid instantiation

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static <T> T check(T value, boolean test, String message) {
        if (!test) {
            throw new IllegalArgumentException(message + ", was " + value);
        }
        return value;
    }
}
