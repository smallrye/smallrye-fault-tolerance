package io.smallrye.faulttolerance;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Workaround for default fallback methods (used e.g. in MP Rest Client).
 * 
 * @author Martin Kouba
 */
class DefaultMethodFallbackProvider {

    static Object getFallback(Method fallbackMethod, ExecutionContextWithInvocationContext ctx)
            throws Throwable {
        // This should work in Java 8
        Class<?> declaringClazz = fallbackMethod.getDeclaringClass();
        Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(declaringClazz)
                .in(declaringClazz)
                .unreflectSpecial(fallbackMethod, declaringClazz)
                .bindTo(ctx.getTarget())
                .invokeWithArguments(ctx.getParameters());
    }

}
