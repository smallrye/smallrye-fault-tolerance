package io.smallrye.faulttolerance.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedMethod;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

public class FaultToleranceMethods {
    public static FaultToleranceMethod create(Class<?> beanClass, AnnotatedMethod<?> method) {
        Set<Class<? extends Annotation>> annotationsPresentDirectly = new HashSet<>();

        FaultToleranceMethod result = new FaultToleranceMethod();

        result.beanClass = beanClass;
        result.method = createMethodDescriptor(method);

        result.applyFaultTolerance = getAnnotation(ApplyFaultTolerance.class, method, annotationsPresentDirectly);
        result.applyGuard = getAnnotation(ApplyGuard.class, method, annotationsPresentDirectly);

        result.asynchronous = getAnnotation(Asynchronous.class, method, annotationsPresentDirectly);
        result.asynchronousNonBlocking = getAnnotation(AsynchronousNonBlocking.class, method, annotationsPresentDirectly);
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
        result.retryWhen = getAnnotation(RetryWhen.class, method, annotationsPresentDirectly);
        result.beforeRetry = getAnnotation(BeforeRetry.class, method, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        try {
            searchForMethods(result, beanClass, method.getJavaMember());
        } catch (PrivilegedActionException e) {
            throw new FaultToleranceDefinitionException(e);
        }

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

    // ---

    public static FaultToleranceMethod create(Class<?> beanClass, Method method) {
        Set<Class<? extends Annotation>> annotationsPresentDirectly = new HashSet<>();

        FaultToleranceMethod result = new FaultToleranceMethod();

        result.beanClass = beanClass;
        result.method = createMethodDescriptor(method);

        result.applyFaultTolerance = getAnnotation(ApplyFaultTolerance.class, method, beanClass, annotationsPresentDirectly);
        result.applyGuard = getAnnotation(ApplyGuard.class, method, beanClass, annotationsPresentDirectly);

        result.asynchronous = getAnnotation(Asynchronous.class, method, beanClass, annotationsPresentDirectly);
        result.asynchronousNonBlocking = getAnnotation(AsynchronousNonBlocking.class, method, beanClass,
                annotationsPresentDirectly);
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
        result.retryWhen = getAnnotation(RetryWhen.class, method, beanClass, annotationsPresentDirectly);
        result.beforeRetry = getAnnotation(BeforeRetry.class, method, beanClass, annotationsPresentDirectly);

        result.annotationsPresentDirectly = annotationsPresentDirectly;

        try {
            searchForMethods(result, beanClass, method);
        } catch (PrivilegedActionException e) {
            throw new FaultToleranceDefinitionException(e);
        }

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

    // ---

    private static void searchForMethods(FaultToleranceMethod result, Class<?> beanClass, Method method)
            throws PrivilegedActionException {
        if (result.fallback != null) {
            FallbackConfig fallbackConfig = FallbackConfigImpl.create(result);
            if (fallbackConfig != null) {
                String fallbackMethod = fallbackConfig.fallbackMethod();
                if (!"".equals(fallbackMethod)) {
                    Class<?> declaringClass = method.getDeclaringClass();
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    Type returnType = method.getGenericReturnType();
                    result.fallbackMethod = createMethodDescriptorIfNotNull(
                            SecurityActions.findFallbackMethod(beanClass, declaringClass, fallbackMethod,
                                    parameterTypes, returnType));
                    result.fallbackMethodsWithExceptionParameter = createMethodDescriptorsIfNotEmpty(
                            SecurityActions.findFallbackMethodsWithExceptionParameter(beanClass, declaringClass, fallbackMethod,
                                    parameterTypes, returnType));
                }
            }
        }

        if (result.beforeRetry != null) {
            BeforeRetryConfig beforeRetryConfig = BeforeRetryConfigImpl.create(result);
            if (beforeRetryConfig != null) {
                String beforeRetryMethod = beforeRetryConfig.methodName();
                if (!"".equals(beforeRetryMethod)) {
                    result.beforeRetryMethod = createMethodDescriptorIfNotNull(
                            SecurityActions.findBeforeRetryMethod(beanClass, method.getDeclaringClass(), beforeRetryMethod));
                }
            }
        }
    }

    private static MethodDescriptor createMethodDescriptorIfNotNull(Method reflectiveMethod) {
        return reflectiveMethod == null ? null : createMethodDescriptor(reflectiveMethod);
    }

    private static List<MethodDescriptor> createMethodDescriptorsIfNotEmpty(Collection<Method> reflectiveMethods) {
        if (reflectiveMethods.isEmpty()) {
            return null;
        }
        List<MethodDescriptor> result = new ArrayList<>(reflectiveMethods.size());
        for (Method reflectiveMethod : reflectiveMethods) {
            result.add(createMethodDescriptor(reflectiveMethod));
        }
        return result;
    }
}
