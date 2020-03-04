package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.interceptor.InvocationContext;

public class InterceptionPoint {
    private final String name;
    private final Class<?> beanClass;
    private final Method method;

    public InterceptionPoint(Class<?> beanClass, InvocationContext invocationContext) {
        this.beanClass = beanClass;
        method = invocationContext.getMethod();
        name = beanClass.getName() + "#" + method.getName();
    }

    public String name() {
        return name;
    }

    public Class<?> beanClass() {
        return beanClass;
    }

    public Method method() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InterceptionPoint that = (InterceptionPoint) o;
        return beanClass.equals(that.beanClass) &&
                method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanClass, method);
    }
}
