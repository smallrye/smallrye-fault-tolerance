package io.smallrye.faulttolerance.core.invocation;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class AsyncSupportRegistry {
    private static final List<AsyncSupport<?, ?>> registry;

    static {
        List<AsyncSupport<?, ?>> list = new ArrayList<>();
        Iterable<AsyncSupport> instances = System.getSecurityManager() != null
                ? AccessController.doPrivileged((PrivilegedAction<Iterable<AsyncSupport>>) AsyncSupportRegistry::load)
                : load();
        for (AsyncSupport<?, ?> instance : instances) {
            list.add(instance);
        }
        registry = Collections.unmodifiableList(list);
    }

    private static ServiceLoader<AsyncSupport> load() {
        return ServiceLoader.load(AsyncSupport.class, AsyncSupport.class.getClassLoader());
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
