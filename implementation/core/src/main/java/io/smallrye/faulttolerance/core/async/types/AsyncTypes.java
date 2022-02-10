package io.smallrye.faulttolerance.core.async.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class AsyncTypes {
    private static final Map<Class<?>, AsyncTypeConverter<?, ?>> registry;

    static {
        Map<Class<?>, AsyncTypeConverter<?, ?>> map = new HashMap<>();
        for (AsyncTypeConverter<?, ?> converter : ServiceLoader.load(AsyncTypeConverter.class,
                AsyncTypes.class.getClassLoader())) {
            map.put(converter.type(), converter);
        }
        registry = Collections.unmodifiableMap(map);
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
