package io.smallrye.faulttolerance.config;

import java.lang.reflect.Type;

final class FallbackValidation {
    /**
     * Returns a boxed variant of {@code type}, if it is a primitive type or the {@code void} pseudo-type.
     * Returns {@code type} itself otherwise.
     *
     * @param type a type
     * @return a boxed variant of {@code type}, or {@code type} itself if boxing isn't needed
     */
    static Type box(Type type) {
        if (type == void.class) {
            return Void.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == char.class) {
            return Character.class;
        } else {
            return type;
        }
    }
}
