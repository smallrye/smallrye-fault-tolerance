package io.smallrye.faulttolerance.core.async.types;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class AsyncTypes {
    private static final Map<Class<?>, AsyncTypeConverter<?, ?>> registry;

    static {
        Map<Class<?>, AsyncTypeConverter<?, ?>> map = new HashMap<>();
        Iterable<AsyncTypeConverter> converters = (System.getSecurityManager() != null)
                ? AccessController.doPrivileged((PrivilegedAction<Iterable<AsyncTypeConverter>>) AsyncTypes::loadConverters)
                : loadConverters();
        for (AsyncTypeConverter<?, ?> converter : converters) {
            map.put(converter.type(), converter);
        }
        registry = Collections.unmodifiableMap(map);
    }

    private static ServiceLoader<AsyncTypeConverter> loadConverters() {
        return ServiceLoader.load(AsyncTypeConverter.class, AsyncTypeConverter.class.getClassLoader());
    }

    public static boolean isKnown(Class<?> type) {
        return registry.containsKey(type);
    }

    public static AsyncTypeConverter<?, ?> get(Class<?> type) {
        return registry.get(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> T toCompletionStageIfRequired(Object value, Class<?> type) {
        AsyncTypeConverter converter = registry.get(type);
        if (converter != null) {
            return (T) converter.toCompletionStage(value);
        } else {
            return (T) value;
        }
    }

    public static Collection<AsyncTypeConverter<?, ?>> allKnown() {
        return registry.values();
    }
}
