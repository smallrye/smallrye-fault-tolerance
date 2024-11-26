package io.smallrye.faulttolerance.core.invocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class AsyncSupportRegistry {
    private static final List<AsyncSupport<?, ?>> registry;

    static {
        List<AsyncSupport<?, ?>> list = new ArrayList<>();
        Iterable<AsyncSupport> instances = ServiceLoader.load(AsyncSupport.class, AsyncSupport.class.getClassLoader());
        for (AsyncSupport<?, ?> instance : instances) {
            list.add(instance);
        }
        registry = Collections.unmodifiableList(list);
    }

    public static boolean isKnown(Class<?>[] parameterTypes, Class<?> returnType) {
        for (AsyncSupport<?, ?> asyncSupport : registry) {
            if (asyncSupport.applies(parameterTypes, returnType)) {
                return true;
            }
        }
        return false;
    }

    public static <V, AT> AsyncSupport<V, AT> get(Class<?>[] parameterTypes, Class<?> returnType) {
        for (AsyncSupport<?, ?> asyncSupport : registry) {
            if (asyncSupport.applies(parameterTypes, returnType)) {
                return (AsyncSupport<V, AT>) asyncSupport;
            }
        }
        return null;
    }

    public static Collection<AsyncSupport<?, ?>> allKnown() {
        return registry;
    }
}
