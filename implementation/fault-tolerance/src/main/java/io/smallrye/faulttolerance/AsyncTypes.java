package io.smallrye.faulttolerance;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.converters.ReactiveTypeConverter;

// TODO reduce statics?
public class AsyncTypes {
    private static final Map<Class<?>, ReactiveTypeConverter<?>> registry;

    static {
        Map<Class<?>, ReactiveTypeConverter<?>> map = new LinkedHashMap<>();
        // include CompletionStage to be able to handle all async types uniformly
        map.put(CompletionStage.class, new CompletionStageConverter());
        for (ReactiveTypeConverter<?> converter : ServiceLoader.load(ReactiveTypeConverter.class)) {
            if (converter.emitAtMostOneItem() || !converter.emitItems()) {
                map.put(converter.type(), converter);
            }
        }
        registry = Collections.unmodifiableMap(map);
    }

    public static boolean isKnown(Class<?> type) {
        return registry.containsKey(type);
    }

    public static ReactiveTypeConverter<?> get(Class<?> type) {
        return registry.get(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> T toCompletionStageIfRequired(Object value, Class<?> type) {
        ReactiveTypeConverter converter = registry.get(type);
        if (converter != null) {
            return (T) converter.toCompletionStage(value);
        } else {
            return (T) value;
        }
    }

    public static Collection<ReactiveTypeConverter<?>> allKnown() {
        return registry.values();
    }

    @SuppressWarnings("rawtypes")
    private static class CompletionStageConverter implements ReactiveTypeConverter<CompletionStage> {
        @SuppressWarnings("unchecked")
        @Override
        public <X> CompletionStage<X> toCompletionStage(CompletionStage instance) {
            return instance;
        }

        @Override
        public <X> Publisher<X> toRSPublisher(CompletionStage instance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <X> CompletionStage fromCompletionStage(CompletionStage<X> cs) {
            return cs;
        }

        @Override
        public <X> CompletionStage fromPublisher(Publisher<X> publisher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<CompletionStage> type() {
            return CompletionStage.class;
        }

        @Override
        public boolean emitItems() {
            return true;
        }

        @Override
        public boolean emitAtMostOneItem() {
            return true;
        }

        @Override
        public boolean supportNullValue() {
            return true;
        }
    }
}
