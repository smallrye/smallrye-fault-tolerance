package io.smallrye.faulttolerance.autoconfig;

import java.lang.reflect.Method;

public class MethodDescriptor {
    public TypeName declaringClass;
    public String name;
    public TypeName[] parameterTypes;
    public TypeName returnType;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(declaringClass.binaryName).append('.').append(name).append('(');
        boolean firstParam = true;
        for (TypeName parameterType : parameterTypes) {
            if (!firstParam) {
                result.append(", ");
            }
            result.append(parameterType.binaryName);
            firstParam = false;
        }
        result.append(')');
        return result.toString();
    }

    public Method reflect() throws NoSuchMethodException, ClassNotFoundException {
        return declaringClass().getDeclaredMethod(name, parameterTypes());
    }

    public Class<?> declaringClass() throws ClassNotFoundException {
        return declaringClass.loadFromTCCL();
    }

    public Class<?>[] parameterTypes() throws ClassNotFoundException {
        Class<?>[] result = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            result[i] = parameterTypes[i].loadFromTCCL();
        }
        return result;
    }

    public Class<?> returnType() throws ClassNotFoundException {
        return returnType.loadFromTCCL();
    }
}
