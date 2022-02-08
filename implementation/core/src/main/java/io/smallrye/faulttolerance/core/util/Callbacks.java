package io.smallrye.faulttolerance.core.util;

import java.util.function.Consumer;

/**
 * Utility methods for wrapping callbacks into variants that don't propagate exceptions.
 */
public class Callbacks {
    public static <T> Consumer<T> wrap(Consumer<T> callback) {
        if (callback == null) {
            return null;
        }

        return value -> {
            try {
                callback.accept(value);
            } catch (Exception ignored) {
            }
        };
    }

    public static Runnable wrap(Runnable callback) {
        if (callback == null) {
            return null;
        }

        return () -> {
            try {
                callback.run();
            } catch (Exception ignored) {
            }
        };
    }

}
