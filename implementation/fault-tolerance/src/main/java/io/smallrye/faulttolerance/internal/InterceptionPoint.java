package io.smallrye.faulttolerance.internal;

import java.lang.reflect.Method;
import java.util.Objects;

public class InterceptionPoint {
    private final String name;
    private final Class<?> beanClass;
    private final Method method;

    public InterceptionPoint(Class<?> beanClass, Method method) {
        this.name = beanClass.getName() + "#" + method.getName();
        this.beanClass = beanClass;
        this.method = method;
    }

    public Class<?> beanClass() {
        return beanClass;
    }

    public Method method() {
        return method;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InterceptionPoint that = (InterceptionPoint) o;
        return beanClass.equals(that.beanClass)
                && method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanClass, method);
    }
}
