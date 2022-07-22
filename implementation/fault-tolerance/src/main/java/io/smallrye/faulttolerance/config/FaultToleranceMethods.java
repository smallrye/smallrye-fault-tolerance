package io.smallrye.faulttolerance.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

public class FaultToleranceMethods {
    public static FaultToleranceMethod create(AnnotatedMethod<?> method) {
        Set<Class<? extends Annotation>> annotationsPresentDirectly = new HashSet<>();

        FaultToleranceMethod result = new FaultToleranceMethod();

        result.beanClass = method.getDeclaringType().getJavaClass();
        result.method = createMethodDescriptor(method);

        result.applyFaultTolerance = getAnnotation(ApplyFaultTolerance.class, method, annotationsPresentDirectly);

        result.asynchronous = getAnnotation(Asynchronous.class, method, annotationsPresentDirectly);
        result.blocking = getAnnotation(Blocking.class, method, annotationsPresentDirectly);
        result.nonBlocking = getAnnotation(NonBlocking.class, method, annotationsPresentDirectly);

        result.bulkhead = getAnnotation(Bulkhead.class, method, annotationsPresentDirectly);
        result.circuitBreaker = getAnnotation(CircuitBreaker.class, method, annotationsPresentDirectly);
        result.circuitBreakerName = getAnnotation(CircuitBreakerName.class, method, annotationsPresentDirectly);
        result.fallback = getAnnotation(Fallback.class, method, annotationsPresentDirectly);
        result.rateLimit = getAnnotation(RateLimit.class, method, annotationsPresentDirectly);
        result.retry = getAnnotation(Retry.class, method, annotationsPresentDirectly);
        result.timeout = getAnnotation(Timeout.class, method, annotationsPresentDirectly);

        result.customBackoff = getAnnotation(CustomBackoff.class, method, annotationsPresentDirectly);
        result.exponentialBackoff = getAnnotation(ExponentialBackoff.class, method, annotationsPresentDirectly);
        result.fibonacciBackoff = getAnnotation(FibonacciBackoff.class, method, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        return result;
    }

    private static MethodDescriptor createMethodDescriptor(AnnotatedMethod<?> cdiMethod) {
        MethodDescriptor result = new MethodDescriptor();
        result.declaringClass = cdiMethod.getJavaMember().getDeclaringClass();
        result.name = cdiMethod.getJavaMember().getName();
        result.parameterTypes = cdiMethod.getJavaMember().getParameterTypes();
        result.returnType = cdiMethod.getJavaMember().getReturnType();
        return result;
    }

    private static <A extends Annotation> A getAnnotation(Class<A> annotationType, AnnotatedMethod<?> cdiMethod,
            Set<Class<? extends Annotation>> directlyPresent) {
        if (cdiMethod.isAnnotationPresent(annotationType)) {
            directlyPresent.add(annotationType);
            return cdiMethod.getAnnotation(annotationType);
        }
        return cdiMethod.getDeclaringType().getAnnotation(annotationType);
    }

    public static FaultToleranceMethod create(Class<?> beanClass, Method method) {
        Set<Class<? extends Annotation>> annotationsPresentDirectly = new HashSet<>();

        FaultToleranceMethod result = new FaultToleranceMethod();

        result.beanClass = beanClass;
        result.method = createMethodDescriptor(method);

        result.applyFaultTolerance = getAnnotation(ApplyFaultTolerance.class, method, beanClass, annotationsPresentDirectly);

        result.asynchronous = getAnnotation(Asynchronous.class, method, beanClass, annotationsPresentDirectly);
        result.blocking = getAnnotation(Blocking.class, method, beanClass, annotationsPresentDirectly);
        result.nonBlocking = getAnnotation(NonBlocking.class, method, beanClass, annotationsPresentDirectly);

        result.bulkhead = getAnnotation(Bulkhead.class, method, beanClass, annotationsPresentDirectly);
        result.circuitBreaker = getAnnotation(CircuitBreaker.class, method, beanClass, annotationsPresentDirectly);
        result.circuitBreakerName = getAnnotation(CircuitBreakerName.class, method, beanClass, annotationsPresentDirectly);
        result.fallback = getAnnotation(Fallback.class, method, beanClass, annotationsPresentDirectly);
        result.rateLimit = getAnnotation(RateLimit.class, method, beanClass, annotationsPresentDirectly);
        result.retry = getAnnotation(Retry.class, method, beanClass, annotationsPresentDirectly);
        result.timeout = getAnnotation(Timeout.class, method, beanClass, annotationsPresentDirectly);

        result.customBackoff = getAnnotation(CustomBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.exponentialBackoff = getAnnotation(ExponentialBackoff.class, method, beanClass, annotationsPresentDirectly);
        result.fibonacciBackoff = getAnnotation(FibonacciBackoff.class, method, beanClass, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        return result;
    }

    private static MethodDescriptor createMethodDescriptor(Method reflectiveMethod) {
        MethodDescriptor result = new MethodDescriptor();
        result.declaringClass = reflectiveMethod.getDeclaringClass();
        result.name = reflectiveMethod.getName();
        result.parameterTypes = reflectiveMethod.getParameterTypes();
        result.returnType = reflectiveMethod.getReturnType();
        return result;
    }

    private static <A extends Annotation> A getAnnotation(Class<A> annotationType, Method reflectiveMethod,
            Class<?> beanClass, Set<Class<? extends Annotation>> directlyPresent) {
        if (reflectiveMethod.isAnnotationPresent(annotationType)) {
            directlyPresent.add(annotationType);
            return reflectiveMethod.getAnnotation(annotationType);
        }
        return getAnnotationFromClass(beanClass, annotationType);
    }

    private static <A extends Annotation> A getAnnotationFromClass(Class<?> clazz, Class<A> annotationType) {
        while (clazz != null) {
            A annotation = clazz.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
