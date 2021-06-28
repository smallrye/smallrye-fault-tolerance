package io.smallrye.faulttolerance.autoconfig;

import java.lang.reflect.Method;

public class MethodDescriptor {
    public Class<?> declaringClass;
    public String name;
    public Class<?>[] parameterTypes;
    public Class<?> returnType;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(declaringClass.getName()).append('.').append(name).append('(');
        boolean firstParam = true;
        for (Class<?> parameterType : parameterTypes) {
            if (!firstParam) {
                result.append(", ");
            }
            result.append(parameterType.getName());
            firstParam = false;
        }
        result.append(')');
        return result.toString();
    }

    public Method reflect() throws NoSuchMethodException {
        return declaringClass.getDeclaredMethod(name, parameterTypes);
    }
}
