package io.smallrye.faulttolerance.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

final class FallbackValidation {
    /**
     * The assignability checks are incomplete and need revision.
     *
     * @param type1
     * @param type2
     * @return {@code true} if type1 is assignable from type2
     */
    static boolean isAssignableFrom(Type type1, Type type2) {
        if (type1 instanceof Class) {
            if (type2 instanceof Class) {
                return isAssignableFrom((Class<?>) type1, (Class<?>) type2);
            }
            if (type2 instanceof ParameterizedType) {
                return false;
            }
            throw new IllegalArgumentException("Unsupported type " + type2);
        }
        if (type1 instanceof ParameterizedType) {
            if (type2 instanceof ParameterizedType) {
                return isAssignableFrom((ParameterizedType) type1, (ParameterizedType) type2);
            }
            if (type2 instanceof Class) {
                return false;
            }
            throw new IllegalArgumentException("Unsupported type " + type2);
        }
        throw new IllegalArgumentException("Unsupported types " + type1 + " and " + type2);
    }

    private static boolean isAssignableFrom(Class<?> type1, Class<?> type2) {
        return type1.isAssignableFrom(type2);
    }

    private static boolean isAssignableFrom(ParameterizedType type1, ParameterizedType type2) {
        final Class<?> rawType1 = (Class<?>) type1.getRawType();
        final Class<?> rawType2 = (Class<?>) type2.getRawType();
        if (!rawType1.equals(rawType2)) {
            return false;
        }
        final Type[] types1 = type1.getActualTypeArguments();
        final Type[] types2 = type2.getActualTypeArguments();
        if (types1.length != types2.length) {
            return false;
        }
        for (int i = 0; i < types1.length; i++) {
            if (!isAssignableFrom(types1[i], types2[i])) {
                return false;
            }
        }
        return true;
    }

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
