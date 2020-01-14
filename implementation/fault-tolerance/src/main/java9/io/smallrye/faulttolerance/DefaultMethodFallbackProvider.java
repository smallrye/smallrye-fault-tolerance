package io.smallrye.faulttolerance;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Workaround for default fallback methods (used e.g. in MP Rest Client).
 * 
 * @author Martin Kouba
 */
class DefaultMethodFallbackProvider {

    static Object getFallback(Method fallbackMethod, ExecutionContextWithInvocationContext ctx)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, Throwable {
        // This should work in Java 9+
        Class<?> declaringClazz = fallbackMethod.getDeclaringClass();
        return MethodHandles.lookup()
                .findSpecial(declaringClazz, fallbackMethod.getName(),
                        MethodType.methodType(fallbackMethod.getReturnType(), fallbackMethod.getParameterTypes()), declaringClazz)
                .bindTo(ctx.getTarget()).invokeWithArguments(ctx.getParameters());
    }

}
